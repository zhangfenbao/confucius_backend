import asyncio
import os
import sys
from multiprocessing import Process
from typing import Awaitable, Callable

import aiohttp
from bots.types import BotCallbacks, BotConfig, BotParams
from bots.voice.bot_error_pipeline import bot_error_pipeline_task
from bots.voice.bot_pipeline import voice_bot_pipeline
from bots.voice.bot_pipeline_runner import BotPipelineRunner
from common.auth import Auth, get_authenticated_db_context
from common.database import DatabaseSessionFactory
from common.models import Service
from fastapi import HTTPException, status
from loguru import logger
from pipecat.pipeline.task import PipelineParams, PipelineTask
from pipecat.transports.services.helpers.daily_rest import (
    DailyRESTHelper,
    DailyRoomParams,
)
from sqlalchemy.ext.asyncio import AsyncSession

MAX_SESSION_TIME = int(os.getenv("SESAME_MAX_VOICE_SESSION_TIME", 15 * 60)) or 15 * 60


async def _cleanup(room_url: str, config: BotConfig, services: dict[str, Service]):
    async with aiohttp.ClientSession() as session:
        debug_room = os.getenv("USE_DEBUG_ROOM", None)
        if debug_room:
            return

        transport_service = services.get("transport")
        transport_api_key = getattr(transport_service, "api_key")
        transport_api_url = (
            getattr(transport_service, "options", {}).get("api_url") or "https://api.daily.co/v1"
        )

        helper = DailyRESTHelper(
            daily_api_key=transport_api_key,
            daily_api_url=transport_api_url,
            aiohttp_session=session,
        )

        try:
            logger.info(f"Deleting room {room_url}")
            await helper.delete_room_by_url(room_url)
        except Exception as e:
            logger.error(f"Bot failed to delete room: {e}")


async def _voice_pipeline_task(
    params: BotParams,
    config: BotConfig,
    services: dict[str, Service],
    room_url: str,
    room_token: str,
    db: AsyncSession,
) -> Callable[[BotCallbacks], Awaitable[PipelineTask]]:
    async def create_task(callbacks: BotCallbacks) -> PipelineTask:
        pipeline = await voice_bot_pipeline(
            params, config, services, callbacks, room_url, room_token, db
        )

        task = PipelineTask(
            pipeline,
            params=PipelineParams(
                allow_interruptions=True,
                enable_metrics=True,
                send_initial_empty_metrics=False,
            ),
        )

        return task

    return create_task


async def _voice_bot_main(
    auth: Auth,
    params: BotParams,
    config: BotConfig,
    services: dict[str, Service],
    room_url: str,
    room_token: str,
):
    subprocess_session_factory = DatabaseSessionFactory()
    async with get_authenticated_db_context(auth, subprocess_session_factory) as db:
        bot_runner = BotPipelineRunner()
        try:
            task_creator = await _voice_pipeline_task(
                params, config, services, room_url, room_token, db
            )
            await bot_runner.start(task_creator)
        except Exception as e:
            logger.error(f"Error running bot: {e}")
            task_creator = await bot_error_pipeline_task(
                room_url, room_token, f"Error running bot: {e}"
            )
            await bot_runner.start(task_creator)

        await _cleanup(room_url, config, services)

        logger.info("Bot has finished. Bye!")
    await subprocess_session_factory.engine.dispose()


def _voice_bot_process(
    auth: Auth,
    params: BotParams,
    config: BotConfig,
    services: dict[str, Service],
    room_url: str,
    room_token: str,
):
    # This is a different process so we need to make sure we have the right log level.
    logger.remove()
    logger.add(sys.stderr, level=os.getenv("SESAME_BOT_LOG_LEVEL", "INFO"))

    asyncio.run(_voice_bot_main(auth, params, config, services, room_url, room_token))


async def voice_bot_create(daily_api_key: str, daily_api_url: str):
    async with aiohttp.ClientSession() as session:
        daily_rest_helper = DailyRESTHelper(
            daily_api_key=daily_api_key,
            daily_api_url=daily_api_url,
            aiohttp_session=session,
        )

        try:
            debug_room = os.getenv("DAILY_USE_DEBUG_ROOM", None)
            if debug_room:
                room = await daily_rest_helper.get_room_from_url(debug_room)
            else:
                room = await daily_rest_helper.create_room(params=DailyRoomParams())
            bot_token = await daily_rest_helper.get_token(room.url, MAX_SESSION_TIME)
            user_token = await daily_rest_helper.get_token(room.url, MAX_SESSION_TIME)
        except Exception as e:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=f"Unable to run bot: {e}"
            )

        return room, user_token, bot_token


def voice_bot_launch(
    auth: Auth,
    params: BotParams,
    config: BotConfig,
    services: dict[str, Service],
    room_url: str,
    room_token: str,
):
    process = Process(
        target=_voice_bot_process, args=(auth, params, config, services, room_url, room_token)
    )
    process.start()
