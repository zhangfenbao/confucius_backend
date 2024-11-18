import os
from typing import Dict, List, Union

from argon2 import PasswordHasher
from common.auth import Auth
from common.models import (
    CreateTokenRequest,
    RevokeTokenRequest,
    Token,
)
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.sql import select, update
from webapp import get_db, get_user

router = APIRouter(prefix="/tokens")

ph = PasswordHasher()


@router.get("", name="Retrieve user tokens", response_model=Dict[str, Union[bool, List[Dict]]])
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


@router.post("/revoke", name="Revoke a user token")
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
