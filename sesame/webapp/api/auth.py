import os
from typing import Optional

from common.database import DatabaseSessionFactory
from common.models import Token, User
from dotenv import load_dotenv
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

load_dotenv(override=True)

AuthProvider = None
auth_provider = os.getenv("SESAME_AUTH_PROVIDER", None)
if auth_provider == "clerk":
    from webapp.integrations.clerk import ClerkAuth

    AuthProvider = ClerkAuth()


router = APIRouter(
    prefix="/auth",
    include_in_schema=AuthProvider is not None,
    tags=["Auth"] if AuthProvider is not None else [],
)


bearer_scheme = HTTPBearer(auto_error=False)

default_session_factory = DatabaseSessionFactory()


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
