import logging
import os
from contextlib import asynccontextmanager

from dotenv import load_dotenv
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def construct_database_url():
    required_vars = {
        "SESAME_DATABASE_PROTOCOL": "postgresql",
        "SESAME_DATABASE_USER": None,
        "SESAME_DATABASE_PASSWORD": None,
        "SESAME_DATABASE_HOST": None,
        "SESAME_DATABASE_PORT": "5432",
        "SESAME_DATABASE_NAME": "sesame",
    }

    missing_vars = [var for var, default in required_vars.items() if not os.getenv(var, default)]

    if missing_vars:
        raise ValueError(
            f"Missing environment variables: {', '.join(missing_vars)}. Please set them in your .env file or environment variables."
        )

    db_url = (
        f"{os.getenv('SESAME_DATABASE_PROTOCOL', 'postgresql')}+"
        f"{os.getenv('SESAME_DATABASE_ASYNC_DRIVER', 'asyncpg')}://"
        f"{os.getenv('SESAME_DATABASE_USER', 'postgres')}:"
        f"{os.getenv('SESAME_DATABASE_PASSWORD', 'postgres')}@"
        f"{os.getenv('SESAME_DATABASE_HOST', 'localhost')}:"
        f"{os.getenv('SESAME_DATABASE_PORT', '5432')}/"
        f"{os.getenv('SESAME_DATABASE_NAME', 'sesame')}"
    )

    print(db_url)

    return db_url


DATABASE_URL = construct_database_url()

engine = create_async_engine(
    DATABASE_URL,
    pool_pre_ping=False,
    echo=bool(int(os.getenv("SESAME_DATABASE_ECHO_OUTPUT", "0"))),
)

AsyncSessionLocal = async_sessionmaker(engine, expire_on_commit=False)


async def get_db_session():
    async with AsyncSessionLocal() as session:
        try:
            yield session
        except SQLAlchemyError:
            await session.rollback()
            raise
        except Exception:
            raise
        finally:
            await session.close()


@asynccontextmanager
async def get_db_session_async_context():
    async with AsyncSessionLocal() as session:
        try:
            yield session
        except SQLAlchemyError as e:
            logger.exception(e)
            await session.rollback()
            raise
        finally:
            await session.close()
