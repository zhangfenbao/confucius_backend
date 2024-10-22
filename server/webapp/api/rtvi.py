import os

from bots.http.bot import http_bot_pipeline
from bots.types import BotConfig, BotParams
from bots.voice.bot import voice_bot_create, voice_bot_launch
from common.auth import Auth, get_authenticated_db_context
from common.models import Conversation
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import JSONResponse, StreamingResponse
from loguru import logger
from sqlalchemy.ext.asyncio import AsyncSession
from webapp.deps import get_db, get_user

router = APIRouter(prefix="/rtvi")


async def _get_config_and_conversation(conversation_id: str, db: AsyncSession):
    conversation = await Conversation.get_conversation_by_id(conversation_id, db)
    if not conversation:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Conversation not found",
        )

    try:
        config_json = conversation.workspace.config
        config = BotConfig.model_validate(config_json)
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Invalid workspace configuration",
        )

    return config, conversation


@router.post("/action", response_class=StreamingResponse)
async def stream_action(
    params: BotParams,
    user: Auth = Depends(get_user),
):
    if not params.conversation_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Missing conversation_id in params",
        )

    async def generate():
        print("USER ID", user.user_id)
        async with get_authenticated_db_context(user) as db:
            config, conversation = await _get_config_and_conversation(params.conversation_id, db)
            messages = [msg.content for msg in conversation.messages]
            gen, task = await http_bot_pipeline(
                params, config, messages, db, conversation.language_code
            )
            async for chunk in gen:
                yield chunk
            await task

    return StreamingResponse(generate(), media_type="text/event-stream")


@router.post("/connect", response_class=JSONResponse)
async def connect(
    params: BotParams, db: AsyncSession = Depends(get_db), user: Auth = Depends(get_user)
):
    if not params.conversation_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Missing conversation_id in params",
        )

    config, conversation = await _get_config_and_conversation(params.conversation_id, db)

    daily_api_key = config.api_keys.get("daily", os.getenv("DAILY_API_KEY", ""))

    if not daily_api_key:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Missing Daily API key",
        )

    room, user_token, bot_token = await voice_bot_create(daily_api_key)

    # Check if we are running on Modal and launch the voice bot as a separate function
    if os.getenv("MODAL_ENV"):
        logger.debug("Spawning voice bot on Modal")
        from app import launch_bot_modal

        launch_bot_modal.spawn(user, params, config, room.url, bot_token)
        # launch_bot_modal.spawn("pew")  # user, params, config, room.url, bot_token)
    else:
        logger.debug("Spawning voice bot as process")
        voice_bot_launch(user, params, config, room.url, bot_token)

    return JSONResponse(
        {
            "room_name": room.name,
            "room_url": room.url,
            "token": user_token,
        }
    )
