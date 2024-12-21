import os
from typing import Optional

from bots.http.bot import http_bot_pipeline
from bots.types import BotConfig, BotParams
from bots.voice.bot import voice_bot_create, voice_bot_launch
from common.auth import Auth, get_authenticated_db_context
from common.errors import ServiceConfigurationError
from common.models import Conversation, Service
from common.service_factory import (
    InvalidServiceTypeError,
    ServiceFactory,
    ServiceType,
    UnsupportedServiceError,
)
from fastapi import APIRouter, Depends, HTTPException, Request, status
from fastapi.responses import JSONResponse, StreamingResponse
from common.utils.logger import get_webapp_logger
from sqlalchemy.ext.asyncio import AsyncSession
from webapp import get_db, get_user

logger = get_webapp_logger()

router = APIRouter(prefix="/rtvi")


async def _get_config_and_conversation(conversation_id: str, db: AsyncSession):
    conversation = await Conversation.get_conversation_by_id(conversation_id, db)
    if not conversation:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Conversation not found",
        )
    try:
        config_json = (
            conversation.workspace.config.copy()
            if getattr(conversation.workspace, "config", None)
            else {}
        )
        config = BotConfig.model_validate(config_json)
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Invalid workspace configuration",
        )

    return config, conversation


async def _validate_services(
    db: AsyncSession,
    config: BotConfig,
    conversation: Conversation,
    service_type_filter: Optional[ServiceType] = None,
):
    try:
        ServiceFactory.validate_service_map(dict(config.services))
    except UnsupportedServiceError as e:
        raise HTTPException(
            status_code=400,
            detail={
                "error": "unsupported_service",
                "service": e.service_name,
                "service_type": e.service_type,
                "valid_services": e.valid_services,
                "message": str(e),
            },
        )
    except InvalidServiceTypeError as e:
        raise HTTPException(
            status_code=400,
            detail={
                "error": "invalid_service_type",
                "service_type": e.service_type,
                "valid_types": e.valid_types,
                "message": str(e),
            },
        )

    # Retrieve API keys for services (workspace and user level)
    try:
        workspace_id = getattr(conversation.workspace, "workspace_id")
        services = await Service.get_services_by_type_map(
            dict(config.services),
            db,
            workspace_id,
            service_type_filter,
        )
    except ServiceConfigurationError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"message": str(e.message), "missing_services": e.missing_services},
        )
    except Exception as e:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(e))

    return services


@router.post("/action", response_class=StreamingResponse)
async def stream_action(
    request: Request,
    params: BotParams,
    user: Auth = Depends(get_user),
):
    if not params.conversation_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Missing conversation_id in params",
        )

    async def generate():
        async with get_authenticated_db_context(user) as db:
            config, conversation = await _get_config_and_conversation(params.conversation_id, db)
            messages = [msg.content for msg in conversation.messages]
            logger.debug(f"Checking cache for services in conversation {params.conversation_id}")
            cache_key = f"services_{params.conversation_id}"
            if cache_key in request.app.state.cache:
                services = request.app.state.cache[cache_key]
            else:
                logger.debug("No cached services. Fetching from database...")
                services = await _validate_services(
                    db, config, conversation, ServiceType.ServiceLLM
                )
                request.app.state.cache[cache_key] = services
            gen, task = await http_bot_pipeline(
                params, config, services, messages, db, conversation.language_code
            )
            async for chunk in gen:
                yield chunk
            await task

    return StreamingResponse(generate(), media_type="text/event-stream")


@router.post("/connect", response_class=JSONResponse)
async def connect(
    params: BotParams, db: AsyncSession = Depends(get_db), user: Auth = Depends(get_user)
):
    logger.info(f"Connecting to conversation {params.conversation_id}")
    if not params.conversation_id:
        logger.error("No conversation ID passed to connect")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Missing conversation_id in params",
        )

    config, conversation = await _get_config_and_conversation(params.conversation_id, db)
    services = await _validate_services(db, config, conversation)

    logger.info(
        "Connecting with services: " + ", ".join(f"{k}: {v}" for k, v in config.services.items())
    )

    if not services.get("transport"):
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Missing transport service configuration",
        )

    logger.info("Retrieving transport service configuration from service factory")

    transport_service = services.get("transport")
    transport_api_key = getattr(transport_service, "api_key", None)

    if not transport_api_key:
        logger.error("Missing API key for transport service")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Missing API key for transport service",
        )
    logger.info(f"Transport API key: {transport_api_key}")
    transport_api_url = ServiceFactory.get_service_defintion(
        ServiceType.ServiceTransport, getattr(transport_service, "service_provider")
    ).default_params.get("api_url")

    if not transport_api_url:
        logger.error("Missing API URL for transport service")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Missing API URL for transport service",
        )

    room, user_token, bot_token = await voice_bot_create(transport_api_key, transport_api_url)
    logger.info(f"Room: {room}, User token: {user_token}, Bot token: {bot_token}")
    # Check if we are running on Modal and launch the voice bot as a separate function
    if os.getenv("MODAL_ENV"):
        logger.debug("Spawning voice bot on Modal")
        try:
            launch_bot_modal = __import__("sesame.modal_app", fromlist=["launch_bot_modal"])
        except ImportError:
            logger.error("Failed to import launch_bot_modal from sesame.modal_app")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Failed to import launch_bot_modal from sesame.modal_app",
            )

        launch_bot_modal.spawn(user, params, config, services, room.url, bot_token)
    else:
        logger.info("Spawning voice bot as process")
        voice_bot_launch(user, params, config, services, room.url, bot_token)

    return JSONResponse(
        {
            "room_name": room.name,
            "room_url": room.url,
            "token": user_token,
        }
    )
