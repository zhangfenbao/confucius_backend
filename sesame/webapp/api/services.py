from uuid import UUID

from common.auth import Auth
from common.encryption import encrypt_with_secret
from common.models import Service, ServiceCreateModel, ServiceModel, ServiceUpdateModel
from common.service_factory import ServiceFactory, ServiceType
from fastapi import APIRouter, Depends, HTTPException, Path
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession
from webapp import get_db, get_user

router = APIRouter(prefix="/services")


@router.get("/supported", response_model=list[str])
async def get_supported_services(
    service_type: str | None = None,
):
    if service_type is None:
        return ServiceFactory.get_available_services()
    try:
        service_type_enum = ServiceType(service_type.lower())
        return ServiceFactory.get_available_services(service_type_enum)
    except KeyError:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid service type. Must be one of: {', '.join(type.name[7:].lower() for type in ServiceType)}",
        )


@router.get("", response_model=list[ServiceModel])
async def get_services(
    db: AsyncSession = Depends(get_db),
    user: Auth = Depends(get_user),
):
    services = await Service.get_services_by_user(db)

    return [ServiceModel.model_validate(service) for service in services]


@router.post("", response_model=ServiceModel)
async def create_service(
    service_data: ServiceCreateModel,
    db: AsyncSession = Depends(get_db),
    user: Auth = Depends(get_user),
):
    encrypted_api_key = encrypt_with_secret(service_data.api_key)

    new_service = Service(
        user_id=user.user_id,
        title=service_data.title,
        service_type=service_data.service_type,
        service_provider=service_data.service_provider,
        api_key=encrypted_api_key,
        workspace_id=service_data.workspace_id,
        options=service_data.options,
    )

    try:
        db.add(new_service)
        await db.commit()
    except IntegrityError as e:
        await db.rollback()
        error_message = str(e).lower()

        if "unique_provider_per_user" in error_message:
            raise HTTPException(
                status_code=409,
                detail=f"A user-level service for provider '{service_data.service_provider}' already exists",
            )
        elif "unique_provider_per_workspace" in error_message:
            raise HTTPException(
                status_code=409,
                detail=f"A service for provider '{service_data.service_provider}' already exists in this workspace",
            )
        elif "services_workspace_id_fkey" in error_message:
            raise HTTPException(status_code=404, detail="Specified workspace does not exist")
        else:
            raise HTTPException(
                status_code=400, detail="Could not create service due to a constraint violation"
            )

    return ServiceModel.model_validate(new_service)


@router.put("/{service_id}", response_model=ServiceModel)
async def update_service(
    service_data: ServiceUpdateModel,
    service_id: UUID = Path(..., title="The ID of the service to update"),
    db: AsyncSession = Depends(get_db),
    user: Auth = Depends(get_user),
):
    result = await db.execute(select(Service).where(Service.service_id == service_id))
    service = result.scalar_one_or_none()

    if not service:
        raise HTTPException(status_code=404, detail="Service not found")

    if service_data.title is not None:
        service.title = service_data.title

    if service_data.api_key is not None:
        service.api_key = encrypt_with_secret(service_data.api_key)

    if service_data.options is not None:
        if isinstance(service.options, dict):
            service.options.update(service_data.options)
        else:
            service.options = service_data.options

    await db.commit()
    await db.refresh(service)

    return ServiceModel.model_validate(service)


@router.delete("/{service_id}", status_code=204)
async def delete_service(
    *,
    service_id: UUID = Path(..., title="The ID of the service to delete"),
    db: AsyncSession = Depends(get_db),
    user: Auth = Depends(get_user),
):
    result = await db.execute(select(Service).where(Service.service_id == service_id))
    service = result.scalar_one_or_none()

    if not service:
        raise HTTPException(status_code=404, detail="Service not found")

    await db.delete(service)
    await db.commit()
