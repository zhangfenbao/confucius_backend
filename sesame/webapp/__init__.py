from typing import Tuple

from common.auth import Auth, get_db_with_token
from fastapi import Depends
from sqlalchemy.ext.asyncio import AsyncSession


# ========================
# Dependencies
# ========================
async def get_db(
    db_and_user: Tuple[AsyncSession, Auth] = Depends(get_db_with_token),
) -> AsyncSession:
    db, _ = db_and_user
    return db


async def get_user(
    db_and_user: Tuple[AsyncSession, Auth] = Depends(get_db_with_token),
) -> Auth:
    _, user = db_and_user
    return user
