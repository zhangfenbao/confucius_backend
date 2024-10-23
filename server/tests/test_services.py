import pytest
from common.encryption import decrypt_with_secret, encrypt_with_secret
from common.models import Service, Workspace
from sqlalchemy import delete, select
from sqlalchemy.exc import IntegrityError

pytestmark = pytest.mark.asyncio(loop_scope="session")


@pytest.fixture(autouse=True)
async def cleanup_services(get_db_with_token):
    """Cleanup any existing services before and after each test"""
    db, auth = get_db_with_token

    # Clean up before test
    await db.execute(delete(Service).where(Service.user_id == auth.user_id))
    await db.execute(delete(Workspace).where(Workspace.user_id == auth.user_id))
    await db.commit()

    yield

    # Clean up after test
    await db.execute(delete(Service).where(Service.user_id == auth.user_id))
    await db.execute(delete(Workspace).where(Workspace.user_id == auth.user_id))
    await db.commit()


async def test_multiple_encryptions():
    test_string = "test-key"

    encrypted1 = encrypt_with_secret(test_string)
    encrypted2 = encrypt_with_secret(test_string)
    encrypted3 = encrypt_with_secret(test_string)

    # All encrypted values should be different (due to Fernet's random component)
    assert encrypted1 != encrypted2 != encrypted3

    # But all should decrypt to the same value
    assert decrypt_with_secret(encrypted1) == test_string
    assert decrypt_with_secret(encrypted2) == test_string
    assert decrypt_with_secret(encrypted3) == test_string

    # Test with different strings
    string1 = encrypt_with_secret("key1")
    string2 = encrypt_with_secret("key2")

    assert decrypt_with_secret(string1) == "key1"
    assert decrypt_with_secret(string2) == "key2"

    # Test encrypt-decrypt cycle with workspace ID present
    test_with_workspace = "workspace-key"
    encrypted_workspace = encrypt_with_secret(test_with_workspace)
    decrypted_workspace = decrypt_with_secret(encrypted_workspace)
    assert decrypted_workspace == test_with_workspace


async def test_get_initial_services(get_db_with_token):
    """
    Test creating a new service
    """
    db, auth = get_db_with_token

    # Get existing services
    user_services = await Service.get_services_by_user(db)
    assert isinstance(user_services, list)


async def test_create_service_and_workspace(get_db_with_token):
    db, auth = get_db_with_token

    # Create a test service
    new_service = Service(
        user_id=auth.user_id,
        title="Test Service",
        service_type="llm",
        service_provider="test",
        api_key=encrypt_with_secret("test-key"),
    )

    db.add(new_service)
    await db.commit()

    assert new_service.service_id is not None

    # Insert another test service with a workspace ID
    new_workspace = Workspace(title="Test workspace", user_id=auth.user_id, config={})
    db.add(new_workspace)
    await db.commit()

    second_service = Service(
        user_id=auth.user_id,
        workspace_id=new_workspace.workspace_id,
        title="Test Service with Workspace",
        service_type="llm",
        service_provider="test",
        api_key=encrypt_with_secret("test-key"),
    )

    db.add(second_service)
    await db.commit()

    assert second_service.workspace_id == new_workspace.workspace_id


async def test_can_create_user_level_service(get_db_with_token):
    """Test creation of a user-level service"""
    db, auth = get_db_with_token

    user_service = Service(
        user_id=auth.user_id,
        title="OpenAI Service",
        service_type="llm",
        service_provider="openai",
        api_key=encrypt_with_secret("test-key-1"),
    )
    db.add(user_service)
    await db.commit()

    result = await db.execute(select(Service).where(Service.service_provider == "openai"))
    services = result.scalars().all()
    assert len(services) == 1
    assert services[0].workspace_id is None


async def test_prevent_duplicate_user_level_service(get_db_with_token):
    """Test that we cannot create duplicate user-level services for the same provider"""
    db, auth = get_db_with_token

    # Create first service
    user_service = Service(
        user_id=auth.user_id,
        title="OpenAI Service",
        service_type="llm",
        service_provider="openai",
        api_key=encrypt_with_secret("test-key-1"),
    )
    db.add(user_service)
    await db.commit()

    # Attempt to create duplicate
    duplicate_service = Service(
        user_id=auth.user_id,
        title="OpenAI Service 2",
        service_type="llm",
        service_provider="openai",
        api_key=encrypt_with_secret("test-key-2"),
    )
    db.add(duplicate_service)

    try:
        await db.flush()
        assert False, "Should have raised IntegrityError"
    except IntegrityError as e:
        assert "unique_provider_per_user" in str(e)
        await db.rollback()


async def test_can_create_workspace_level_service(get_db_with_token):
    """Test creation of a workspace-level service"""
    db, auth = get_db_with_token

    # Create workspace
    workspace = Workspace(title="Test Workspace", user_id=auth.user_id, config={})
    db.add(workspace)
    await db.commit()
    await db.refresh(workspace)

    workspace_service = Service(
        user_id=auth.user_id,
        workspace_id=workspace.workspace_id,
        title="OpenAI Workspace Service",
        service_type="llm",
        service_provider="openai",
        api_key=encrypt_with_secret("test-key-3"),
    )
    db.add(workspace_service)
    await db.commit()

    result = await db.execute(select(Service).where(Service.workspace_id == workspace.workspace_id))
    services = result.scalars().all()
    assert len(services) == 1
    assert services[0].workspace_id == workspace.workspace_id


async def test_prevent_duplicate_workspace_service(get_db_with_token):
    """Test that we cannot create duplicate services in the same workspace"""
    db, auth = get_db_with_token

    # Create workspace
    workspace = Workspace(title="Test Workspace", user_id=auth.user_id, config={})
    db.add(workspace)
    await db.commit()
    await db.refresh(workspace)

    # Create first service
    workspace_service = Service(
        user_id=auth.user_id,
        workspace_id=workspace.workspace_id,
        title="OpenAI Workspace Service",
        service_type="llm",
        service_provider="openai",
        api_key=encrypt_with_secret("test-key-3"),
    )
    db.add(workspace_service)
    await db.commit()

    # Attempt to create duplicate
    duplicate_service = Service(
        user_id=auth.user_id,
        workspace_id=workspace.workspace_id,
        title="OpenAI Workspace Service 2",
        service_type="llm",
        service_provider="openai",
        api_key=encrypt_with_secret("test-key-4"),
    )
    db.add(duplicate_service)

    try:
        await db.flush()
        assert False, "Should have raised IntegrityError"
    except IntegrityError as e:
        assert "unique_provider_per_workspace" in str(e)
        await db.rollback()


async def test_allow_same_provider_different_workspaces(get_db_with_token):
    """Test that the same provider can be used in different workspaces"""
    db, auth = get_db_with_token

    # Create first workspace and service
    workspace1 = Workspace(title="Test Workspace 1", user_id=auth.user_id, config={})
    db.add(workspace1)
    await db.commit()
    await db.refresh(workspace1)

    service1 = Service(
        user_id=auth.user_id,
        workspace_id=workspace1.workspace_id,
        title="OpenAI Workspace 1",
        service_type="llm",
        service_provider="openai",
        api_key=encrypt_with_secret("test-key-5"),
    )
    db.add(service1)
    await db.commit()

    # Create second workspace and service
    workspace2 = Workspace(title="Test Workspace 2", user_id=auth.user_id, config={})
    db.add(workspace2)
    await db.commit()
    await db.refresh(workspace2)

    service2 = Service(
        user_id=auth.user_id,
        workspace_id=workspace2.workspace_id,
        title="OpenAI Workspace 2",
        service_type="llm",
        service_provider="openai",
        api_key=encrypt_with_secret("test-key-6"),
    )
    db.add(service2)
    await db.commit()

    # Verify we have both services
    result = await db.execute(
        select(Service)
        .where(Service.service_provider == "openai")
        .where(Service.workspace_id.isnot(None))
    )
    services = result.scalars().all()
    assert len(services) == 2
    assert len(set(s.workspace_id for s in services)) == 2
