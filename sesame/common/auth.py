from contextlib import asynccontextmanager
from typing import AsyncGenerator, Tuple

from common.database import DatabaseSessionFactory
from common.models import Token
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession
from common.utils.logger import get_webapp_logger

logger = get_webapp_logger()

bearer_scheme = HTTPBearer()
default_session_factory = DatabaseSessionFactory()


class Auth:
    def __init__(self, user_id: str):
        self.user_id = user_id


async def authenticate(token: str, session: AsyncSession) -> Auth:
    result = await Token.get_token(token, session)
    # logger.info(f"authenticate result: {result.user_id} {result.token} {result.revoked} {result.expires_at}")
    if not result:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or revoked authentication token",
        )
    return Auth(str(result.user_id))


def verify_token(credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme)):
    if credentials.credentials is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing authentication token",
        )
    return credentials.credentials


def create_db_dependency(factory: DatabaseSessionFactory = default_session_factory):
    async def get_db_with_token(
        token: str = Depends(verify_token),
    ) -> AsyncGenerator[Tuple[AsyncSession, Auth], None]:
        async with factory() as session:
            authenticated_user: Auth = await authenticate(token, session)
            if not authenticated_user:
                raise ValueError("Auth key cannot be empty or None.")
            await session.execute(
                text("SELECT set_current_user_id(:user_id)"),
                {"user_id": authenticated_user.user_id},
            )
            yield session, authenticated_user

    return get_db_with_token


get_db_with_token = create_db_dependency()


@asynccontextmanager
async def get_authenticated_db_context(
    auth: Auth,
    db_factory: DatabaseSessionFactory = default_session_factory,
) -> AsyncGenerator[AsyncSession, None]:
    async with db_factory() as db:
        async with db.begin():
            await db.execute(
                text("SELECT set_current_user_id(:user_id)"), {"user_id": auth.user_id}
            )
            yield db
