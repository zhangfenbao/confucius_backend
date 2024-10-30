import os
from typing import Dict, List, Union

from argon2 import PasswordHasher
from argon2.exceptions import VerifyMismatchError
from common.auth import Auth
from common.database import DatabaseSessionFactory
from common.models import (
    CreateTokenRequest,
    RevokeTokenRequest,
    Token,
    UserLoginModel,
)
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.sql import select, update
from sqlalchemy.sql.expression import text
from webapp import get_db, get_user

router = APIRouter(prefix="/users")

ph = PasswordHasher()
default_session_factory = DatabaseSessionFactory()


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


@router.post(
    "/tokens", name="Retrieve user tokens", response_model=Dict[str, Union[bool, List[Dict]]]
)
async def get_tokens(
    db: AsyncSession = Depends(get_db),
    user: Auth = Depends(get_user),
):
    tokens_result = await db.execute(
        select(Token).where(Token.user_id == user.user_id, Token.revoked.is_(False))
    )
    tokens = tokens_result.scalars().all()
    return {
        "success": True,
        "tokens": [
            {
                "token_id": str(token.token_id),  # Convert UUID to string
                "token": token.token,
                "title": token.title,
                "created_at": token.created_at,
                "expires_at": token.expires_at,
                "revoked": token.revoked,
            }
            for token in tokens
        ],
    }


@router.post("/token", name="Create a user token")
async def create_token(
    token_create: CreateTokenRequest,
    db: AsyncSession = Depends(get_db),
    user: Auth = Depends(get_user),
):
    try:
        token = await Token.create_token_for_user(
            user.user_id,
            db,
            title=token_create.title or "Default Token",
            expiration_minutes=int(os.getenv("SESAME_TOKEN_EXPIRY", 525600)),
        )
        await db.commit()
    except IntegrityError:
        raise HTTPException(status_code=500, detail="Failed to create user token")

    return {"success": True, "token": token.token}


@router.post("/revoke_token", name="Revoke a user token")
async def revoke_token(
    revoke_data: RevokeTokenRequest,
    db: AsyncSession = Depends(get_db),
    user: Auth = Depends(get_user),
):
    if revoke_data.token:
        result = await db.execute(
            update(Token)
            .where(Token.token == revoke_data.token)
            .where(Token.user_id == user.user_id)
            .values(revoked=True)
        )
        await db.commit()
        if result.rowcount == 0:
            return {
                "success": False,
                "message": "Token not found or does not belong to current user.",
            }
        return {"success": True, "message": "Token revoked successfully."}
    else:
        await db.execute(update(Token).where(Token.user_id == user.user_id).values(revoked=True))
        await db.commit()
        return {"success": True, "message": "All user tokens revoked successfully."}


async def _authenticate_user(credentials: UserLoginModel, db_session: AsyncSession):
    result = await db_session.execute(
        text("SELECT * FROM check_rate_limit(:username, :max_attempts, :window_minutes)"),
        {
            "username": credentials.username,
            "max_attempts": int(os.getenv("SESAME_MAX_LOGIN_ATTEMPTS", 5)),
            "window_minutes": int(os.getenv("SESAME_RATE_LIMIT_WINDOW_MINUTES", 15)),
        },
    )
    rate_limit_ok = result.scalar_one()
    await db_session.commit()

    if not rate_limit_ok:
        raise Exception("Rate limit exceeded. Please try again later.")

    result = await db_session.execute(
        text("SELECT * FROM get_user_for_login(:username)"), {"username": credentials.username}
    )
    user = result.fetchone()

    if not user:
        raise Exception("Invalid username or password")

    try:
        ph.verify(user.password_hash, credentials.password)
    except VerifyMismatchError:
        raise Exception("Invalid username or password")

    await db_session.execute(
        text("SELECT set_current_user_id(:user_id)"), {"user_id": user.user_id}
    )

    await db_session.execute(
        text("DELETE FROM login_attempts WHERE username = :username"),
        {"username": credentials.username},
    )

    await db_session.commit()

    return user
