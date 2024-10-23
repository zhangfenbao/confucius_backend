from common.auth import Auth
from common.encryption import encrypt_with_secret
from common.models import Service, ServiceCreateModel, ServiceModel
from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from webapp import get_db, get_user

router = APIRouter(prefix="/services")


@router.get("", response_model=list[ServiceModel])
async def get_services(
    db: AsyncSession = Depends(get_db),
    user: Auth = Depends(get_user),
):
    services = await Service.get_services_by_user(user.user_id, db)

    return [ServiceModel.model_validate(service) for service in services]


@router.post("", response_model=ServiceModel)
async def create_service(
    service_data: ServiceCreateModel,
    db: AsyncSession = Depends(get_db),
    user: Auth = Depends(get_user),
):
    encrypted_api_key = encrypt_with_secret(service_data.api_key)

    new_service = Service(
        title=service_data.title,
        service_type=service_data.service_type,
        service_provider=service_data.service_provider,
        api_key=encrypted_api_key,
        workspace_id=service_data.workspace_id,
        options=service_data.options,
    )

    db.add(new_service)
    await db.commit()

    return ServiceModel.model_validate(new_service)
