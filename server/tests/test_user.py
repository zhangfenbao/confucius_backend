import pytest
from httpx import AsyncClient

pytestmark = pytest.mark.asyncio(loop_scope="session")


async def test_endpoint(async_client):
    """
    Test an example endpoint, using the test database session.
    """
    response = await async_client.get("/")
    assert response.status_code == 200


async def test_login_endpoint(async_client):
    """
    Test the /users/login endpoint with body parameters.
    """
    # Test with wrong password
    login_data = {"username": "test", "password": "wrongpassword"}
    response = await async_client.post("/api/users/login", json=login_data)
    assert response.status_code == 400

    # Test with correct password
    login_data = {"username": "test", "password": "testtest"}
    response = await async_client.post("/api/users/login", json=login_data)
    assert response.status_code == 200
    response_json = response.json()
    assert response_json["success"] is True


async def test_get_tokens_endpoint(authorized_client: AsyncClient):
    """
    Test the /tokens endpoint with valid credentials and expect tokens to be returned.
    """
    response = await authorized_client.post("/api/users/tokens")
    assert (
        response.status_code == 200
    ), f"Expected status code 200, but got {response.status_code}. Response: {response.text}"
    response_json = response.json()
    assert response_json["success"] is True
    assert isinstance(response_json["tokens"], list)


async def test_create_token(authorized_client: AsyncClient):
    data = {"title": "Test token"}

    response = await authorized_client.post("/api/users/token", json=data)
    assert response.status_code == 200

    response_json = response.json()
    assert response_json["success"] is True, f"Expected 'success': True, but got {response_json}"
    assert "token" in response_json, f"Expected 'token' in response, but got {response_json}"
    assert isinstance(
        response_json["token"], str
    ), f"Expected 'token' to be a string, but got {type(response_json['token'])}"


async def test_revoke_token(authorized_client: AsyncClient):
    data = {"title": "Test token"}

    response = await authorized_client.post("/api/users/token", json=data)
    assert response.status_code == 200

    response_json = response.json()
    token = response_json["token"]
    assert token

    # Revoke single token
    data = {"token": token}
    response = await authorized_client.post("/api/users/revoke_token", json=data)
    assert response.status_code == 200
    response_json = response.json()
    assert response_json["success"] is True

    # Create new token
    data = {"title": "Test token 2"}
    response = await authorized_client.post("/api/users/token", json=data)
    assert response.status_code == 200

    # Revoke all tokens
    response = await authorized_client.post("/api/users/revoke_token", json={})
    assert response.status_code == 200
    response_json = response.json()
    assert response_json["success"] is True

    # Check all tokens are gone
    response = await authorized_client.post("/api/users/tokens")
    response_json = response.json()
    assert isinstance(response_json["tokens"], list)
    assert response_json["tokens"] == []

    # Create new token (for future tests)
    data = {"title": "Test token 3"}
    response = await authorized_client.post("/api/users/token", json=data)
    assert response.status_code == 200
