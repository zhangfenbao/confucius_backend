import pytest
from common.encryption import decrypt_with_secret, encrypt_with_secret
from common.models import Service, Workspace
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError

pytestmark = pytest.mark.asyncio(loop_scope="session")


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


async def test_service_uniqueness_constraints(get_db_with_token):
    db, auth = get_db_with_token

    # Create a workspace first and commit it
    workspace = Workspace(title="Test Workspace", user_id=auth.user_id, config={})
    db.add(workspace)
    await db.commit()
    await db.refresh(workspace)
    workspace_id = workspace.workspace_id
    assert workspace_id is not None

    # Test 1: Create a user-level service
    user_service = Service(
        user_id=auth.user_id,
        title="OpenAI Service",
        service_type="llm",
        service_provider="openai",
        api_key=encrypt_with_secret("test-key-1"),
    )
    db.add(user_service)
    await db.commit()

    # Verify first service
    result = await db.execute(select(Service).where(Service.service_provider == "openai"))
    services = result.scalars().all()
    assert len(services) == 1, "Should have one user-level service"

    # Test 2: Try to create another user-level service with same provider
    duplicate_user_service = Service(
        user_id=auth.user_id,
        title="OpenAI Service 2",
        service_type="llm",
        service_provider="openai",
        api_key=encrypt_with_secret("test-key-2"),
    )
    db.add(duplicate_user_service)
    try:
        await db.flush()
        assert False, "Should have raised IntegrityError"
    except IntegrityError as e:
        assert "unique_provider_per_user" in str(e)
        await db.rollback()

    # Verify workspace still exists and get fresh reference
    result = await db.execute(select(Workspace).where(Workspace.workspace_id == workspace_id))
    workspace = result.scalar_one()
    assert workspace.workspace_id == workspace_id

    # Test 3: Create a workspace-level service
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

    # Verify after workspace service
    result = await db.execute(select(Service).where(Service.service_provider == "openai"))
    services = result.scalars().all()
    assert len(services) == 2, "Should have user-level and workspace service"

    # Test 4: Try to create another service with same provider in same workspace
    duplicate_workspace_service = Service(
        user_id=auth.user_id,
        workspace_id=workspace.workspace_id,
        title="OpenAI Workspace Service 2",
        service_type="llm",
        service_provider="openai",
        api_key=encrypt_with_secret("test-key-4"),
    )
    db.add(duplicate_workspace_service)
    try:
        await db.flush()
        assert False, "Should have raised IntegrityError"
    except IntegrityError as e:
        assert "unique_provider_per_workspace" in str(e)
        await db.rollback()

    # Test 5: Create service in different workspace
    workspace2 = Workspace(title="Test Workspace 2", user_id=auth.user_id, config={})
    db.add(workspace2)
    await db.commit()
    await db.refresh(workspace2)
    assert workspace2.workspace_id is not None

    different_workspace_service = Service(
        user_id=auth.user_id,
        workspace_id=workspace2.workspace_id,
        title="OpenAI Different Workspace",
        service_type="llm",
        service_provider="openai",
        api_key=encrypt_with_secret("test-key-5"),
    )
    db.add(different_workspace_service)
    await db.commit()

    # Verify final state with detailed counts
    result = await db.execute(
        select(Service).where(Service.service_provider == "openai").order_by(Service.created_at)
    )
    services = result.scalars().all()

    user_level_services = [s for s in services if s.workspace_id is None]
    workspace_services = [s for s in services if s.workspace_id is not None]

    assert len(services) == 3, f"Expected 3 services, got {len(services)}"
    assert (
        len(user_level_services) == 1
    ), f"Expected 1 user-level service, got {len(user_level_services)}"
    assert (
        len(workspace_services) == 2
    ), f"Expected 2 workspace services, got {len(workspace_services)}"
