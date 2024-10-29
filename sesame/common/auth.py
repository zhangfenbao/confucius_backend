from contextlib import asynccontextmanager
from typing import AsyncGenerator

from common.database import get_db_session, get_db_session_async_context
from common.models import Token
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

bearer_scheme = HTTPBearer()


# Example base Auth class. This can be extended to include more user information
class Auth:
    def __init__(self, user_id: str):
        self.user_id = user_id


# Authenticate the user with the provided auth key
async def authenticate(token: str, session) -> Auth:
    result = await Token.get_token(token, session)

    if not result:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or revoked authentication token",
        )
    return Auth(result.user_id)


# Get bearer token from the request
def verify_token(credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme)):
    if credentials.credentials is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing authentication token",
        )

    return credentials.credentials


# FastAPI dependency to get the database session and apply authentication
async def get_db_with_token(
    token: HTTPAuthorizationCredentials = Depends(verify_token),
) -> AsyncGenerator[tuple[AsyncSession, Auth], None]:
    async for session in get_db_session():
        authenticated_user: Auth = await authenticate(token, session)
        if not authenticated_user:
            raise ValueError("Auth key cannot be empty or None.")
        await session.execute(
            text("SELECT set_current_user_id(:user_id)"), {"user_id": authenticated_user.user_id}
        )
        yield session, authenticated_user


# Authenticated database context
@asynccontextmanager
async def get_authenticated_db_context(auth: Auth):
    async with get_db_session_async_context() as db:
        async with db.begin():
            await db.execute(
                text("SELECT set_current_user_id(:user_id)"), {"user_id": auth.user_id}
            )
            yield db
