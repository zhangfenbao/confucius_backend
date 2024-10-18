import os
import uuid
from typing import AsyncGenerator, Callable
from urllib.parse import urlparse, urlunparse

import psycopg2
import pytest
from argon2 import PasswordHasher
from common.auth import Auth, authenticate
from common.database import get_db_session
from dotenv import load_dotenv
from httpx import ASGITransport, AsyncClient
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from webapp.deps import get_db, get_user
from webapp.main import app

load_dotenv(override=True)

DATABASE_URL = os.getenv("SESAME_DATABASE_ADMIN_URL")
if not DATABASE_URL:
    raise ValueError(
        "SESAME_DATABASE_ADMIN_URL is not set. Please set it in your .env file or environment variables."
    )

if DATABASE_URL.startswith("postgresql://"):
    DATABASE_URL = DATABASE_URL.replace("postgresql://", "postgresql+asyncpg://")

TEST_DATABASE_NAME = "test_db"


def get_database_url_with_new_db(db_name):
    parsed_url = urlparse(DATABASE_URL)
    new_path = f"/{db_name}"
    return urlunparse(parsed_url._replace(path=new_path))


def load_schema_psycopg2(schema_file_path, db_url, username="test", password="testtest"):
    """
    Uses psycopg2 to apply the schema from a SQL file to the given database.
    This avoids asyncpg's restriction on multiple commands in one statement.
    """
    parsed_url = urlparse(db_url)
    host = parsed_url.hostname
    port = parsed_url.port or 5432
    user = parsed_url.username
    db_password = parsed_url.password
    db_name = parsed_url.path.lstrip("/")

    connection = psycopg2.connect(
        host=host,
        port=port,
        user=user,
        password=db_password,
        dbname=db_name,
    )
    cursor = connection.cursor()

    with open(schema_file_path, "r") as schema_file:
        schema_sql = schema_file.read()

    schema_sql = schema_sql.replace("%%USER%%", "sesame")
    schema_sql = schema_sql.replace("%%PASSWORD%%", "test")

    cursor.execute(schema_sql)

    user_id = uuid.uuid4().hex
    ph = PasswordHasher()
    password_hash = ph.hash(password)

    insert_user_query = """
    INSERT INTO users (user_id, username, password_hash)
    VALUES (%s, %s, %s);
    """
    cursor.execute(insert_user_query, (user_id, username, password_hash))

    connection.commit()

    cursor.close()
    connection.close()


@pytest.fixture(scope="session")
def create_test_database():
    postgres_url = get_database_url_with_new_db("postgres")
    sync_postgres_url = postgres_url.replace("postgresql+asyncpg://", "postgresql://")

    # Create test database
    connection = psycopg2.connect(sync_postgres_url.replace("/postgres", "/postgres"))
    connection.autocommit = True
    cursor = connection.cursor()

    cursor.execute(f"DROP DATABASE IF EXISTS {TEST_DATABASE_NAME}")
    cursor.execute(f"CREATE DATABASE {TEST_DATABASE_NAME}")

    cursor.close()
    connection.close()

    test_db_url = get_database_url_with_new_db(TEST_DATABASE_NAME)
    load_schema_psycopg2("../database/schema.sql", test_db_url)

    yield test_db_url

    connection = psycopg2.connect(sync_postgres_url.replace("/postgres", "/postgres"))
    connection.autocommit = True
    cursor = connection.cursor()

    cursor.execute(f"""
    SELECT pg_terminate_backend(pg_stat_activity.pid)
    FROM pg_stat_activity
    WHERE pg_stat_activity.datname = '{TEST_DATABASE_NAME}'
    AND pid <> pg_backend_pid();
    """)

    cursor.execute(f"DROP DATABASE IF EXISTS {TEST_DATABASE_NAME}")
    cursor.close()
    connection.close()


@pytest.fixture(scope="session")
async def db_engine(create_test_database):
    engine = create_async_engine(create_test_database, echo=False)
    yield engine
    await engine.dispose()


@pytest.fixture(scope="module")
async def db_session(db_engine):
    async_session = async_sessionmaker(db_engine, expire_on_commit=False, class_=AsyncSession)
    async with async_session() as session:
        yield session
        await session.rollback()


@pytest.fixture(scope="module")
async def async_client(db_session: AsyncSession):
    app.dependency_overrides[get_db_session] = lambda: db_session
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://testserver") as client:
        yield client
    app.dependency_overrides.clear()


@pytest.fixture(scope="module")
async def auth_token(async_client) -> Callable[[], str]:
    token = None

    async def get_valid_token():
        nonlocal token
        if token is None:
            login_data = {"username": "test", "password": "testtest"}
            response = await async_client.post("/api/users/login", json=login_data)
            assert (
                response.status_code == 200
            ), f"Login failed with status {response.status_code}: {response.text}"
            response_json = response.json()
            assert "token" in response_json, f"Token not found in login response: {response_json}"
            token = response_json["token"]
        return token

    return get_valid_token


@pytest.fixture(scope="module")
async def get_db_with_token(
    db_session: AsyncSession,
    auth_token: Callable[[], str],
) -> AsyncGenerator[tuple[AsyncSession, Auth], None]:
    token_str = await auth_token()
    authenticated_user: Auth = await authenticate(token_str, db_session)
    if not authenticated_user:
        raise ValueError("Auth key cannot be empty or None.")
    await db_session.execute(text(f"SET app.current_user_id = '{authenticated_user.user_id}'"))
    yield db_session, authenticated_user


@pytest.fixture(scope="module")
async def override_get_db(get_db_with_token):
    async def _get_db():
        yield get_db_with_token[0]

    return _get_db


@pytest.fixture(scope="module")
async def override_get_user(get_db_with_token):
    async def _get_user():
        return get_db_with_token[1]

    return _get_user


@pytest.fixture(scope="module")
async def authorized_client(
    async_client, auth_token: Callable[[], str], override_get_db, override_get_user
):
    app.dependency_overrides[get_db_session] = override_get_db
    app.dependency_overrides[get_db] = override_get_db
    app.dependency_overrides[get_user] = override_get_user

    async_client.headers["Authorization"] = f"Bearer {await auth_token()}"
    return async_client


# Use loop_scope instead of scope
pytestmark = pytest.mark.asyncio(loop_scope="session")
