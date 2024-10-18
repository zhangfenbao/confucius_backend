import pytest
from httpx import AsyncClient

pytestmark = pytest.mark.asyncio(loop_scope="session")


async def test_get_workspaces(authorized_client: AsyncClient):
    response = await authorized_client.get("/api/workspaces")
    assert response.status_code == 200
    response_json = response.json()
    assert isinstance(response_json, list)
