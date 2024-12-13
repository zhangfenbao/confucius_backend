import os
from typing import Optional

from argon2 import PasswordHasher
from argon2.exceptions import VerifyMismatchError
from common.database import DatabaseSessionFactory
from common.models import Token, User, UserLoginModel
from dotenv import load_dotenv
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession

load_dotenv(override=True)

ph = PasswordHasher()

AuthProvider = None
auth_provider = os.getenv("SESAME_AUTH_PROVIDER", None)
if auth_provider == "clerk":
    from webapp.integrations.clerk import ClerkAuth

    AuthProvider = ClerkAuth()


router = APIRouter(
    prefix="/auth",
    # include_in_schema=AuthProvider is not None,
    # tags=["Auth"] if AuthProvider is not None else [],
    include_in_schema=True,
    tags=["Auth"],
)


bearer_scheme = HTTPBearer(auto_error=False)

default_session_factory = DatabaseSessionFactory()


async def _authenticate_user(credentials: UserLoginModel, db_session: AsyncSession):
    result = await db_session.execute(
        text("SELECT * FROM check_rate_limit(:email, :max_attempts, :window_minutes)"),
        {
            "email": credentials.email,
            "max_attempts": int(os.getenv("SESAME_MAX_LOGIN_ATTEMPTS", 5)),
            "window_minutes": int(os.getenv("SESAME_RATE_LIMIT_WINDOW_MINUTES", 15)),
        },
    )
    rate_limit_ok = result.scalar_one()
    await db_session.commit()

    if not rate_limit_ok:
        raise Exception("Rate limit exceeded. Please try again later.")

    result = await db_session.execute(
        text("SELECT * FROM get_user_for_login(:email)"), {"email": credentials.email}
    )
    user = result.fetchone()

    if not user:
        raise Exception("Invalid email or password")

    try:
        ph.verify(user.password_hash, credentials.password)
    except VerifyMismatchError:
        raise Exception("Invalid email or password")

    await db_session.execute(
        text("SELECT set_current_user_id(:user_id)"), {"user_id": user.user_id}
    )

    await db_session.execute(
        text("DELETE FROM login_attempts WHERE email = :email"),
        {"email": credentials.email},
    )

    await db_session.commit()

    return user


@router.post("/login", name="Login with credentials")
async def login_with_credentials(
    credentials: UserLoginModel,
):
    async with default_session_factory() as db_session:
        try:
            user = await _authenticate_user(credentials, db_session)
        except Exception as e:
            raise HTTPException(status_code=400, detail=str(e))

        # Get or create a token for the user
        tokens = await db_session.execute(
            select(Token).where(Token.user_id == user.user_id, Token.revoked.is_(False)).limit(1)
        )
        token = tokens.scalar_one_or_none()

        if not token:
            token = await Token.create_token_for_user(
                user.user_id,
                db_session,
                title="Default Token",
                expiration_minutes=int(os.getenv("SESAME_TOKEN_EXPIRY", 525600)),
            )
            await db_session.commit()

        return {"success": True, "token": token.token}


@router.get("/token")
async def get_token(credentials: Optional[HTTPAuthorizationCredentials] = Depends(bearer_scheme)):
    if not AuthProvider or credentials is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="No authentication token provided",
            headers={"WWW-Authenticate": "Bearer"},
        )

    if not credentials.credentials:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    try:
        user_email = await AuthProvider.verify_session(credentials.credentials)
        if not user_email:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid or expired session token",
                headers={"WWW-Authenticate": "Bearer"},
            )

        async with default_session_factory() as db_session:
            try:
                user = await User.authenticate_by_email(db=db_session, email=user_email)
                if not user or not user.user_id:
                    raise
            except Exception:
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED, detail="Unable to authenticate user"
                )

            try:
                token = await Token.get_or_create_token_for_user(
                    db=db_session,
                    user_id=user.user_id,
                    title=f"{str(auth_provider).capitalize()} Token",
                )
                return {"success": True, "token": token.token}
            except Exception:
                raise HTTPException(
                    status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Error creating token"
                )

    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=str(e),
            headers={"WWW-Authenticate": "Bearer"},
        )

    return credentials.credentials
