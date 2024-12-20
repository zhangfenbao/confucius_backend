from typing import Any, cast

from bots.persistent_context import PersistentContext
from bots.rtvi import create_rtvi_processor
from bots.types import BotCallbacks, BotConfig, BotParams
from common.models import Conversation, Message, Service
from common.service_factory import ServiceFactory, ServiceType
from common.utils.logger import get_bot_logger
from openai._types import NOT_GIVEN
from sqlalchemy.ext.asyncio import AsyncSession

from pipecat.audio.vad.silero import SileroVADAnalyzer
from pipecat.audio.vad.vad_analyzer import VADParams
from pipecat.pipeline.pipeline import Pipeline
from pipecat.processors.frame_processor import FrameDirection
from pipecat.processors.frameworks.rtvi import (
    RTVIBotLLMProcessor,
    RTVIBotTranscriptionProcessor,
    RTVIBotTTSProcessor,
    RTVISpeakingProcessor,
    RTVIUserTranscriptionProcessor,
)
from pipecat.services.ai_services import (
    LLMService,
    OpenAILLMContext,
)
from pipecat.transports.services.daily import DailyParams, DailyTransport
import logging

logger = get_bot_logger()

for logger_name in logging.root.manager.loggerDict.keys():
    if logger_name.startswith('sqlalchemy'):
        logging.getLogger(logger_name).setLevel(logging.ERROR)

async def voice_bot_pipeline(
    params: BotParams,
    config: BotConfig,
    services: dict[str, Service],
    callbacks: BotCallbacks,
    room_url: str,
    room_token: str,
    db: AsyncSession,
) -> Pipeline:
    if "transport" not in services:
        raise Exception("Service `llm` not available in provided services.")
    if "llm" not in services:
        raise Exception("Service `llm` not available in provided services.")
    if "tts" not in services:
        raise Exception("Service `tts` not available in provided services.")
    if "stt" not in services:
        raise Exception("Service `stt` not available in provided services.")

    llm = cast(
        LLMService,
        ServiceFactory.get_service(
            str(services["llm"].service_provider),
            ServiceType.ServiceLLM,
            str(services["llm"].api_key),
            getattr(services["llm"], "options"),
        ),
    )
    tts = cast(
        LLMService,
        ServiceFactory.get_service(
            str(services["tts"].service_provider),
            ServiceType.ServiceTTS,
            str(services["tts"].api_key),
            getattr(services["tts"], "options"),
        ),
    )
    stt = cast(
        LLMService,
        ServiceFactory.get_service(
            str(services["stt"].service_provider),
            ServiceType.ServiceSTT,
            str(services["stt"].api_key),
            getattr(services["stt"], "options"),
        ),
    )

    # Daily API is used in dial-in case only
    transport = DailyTransport(
        room_url,
        room_token,
        "Open Sesame",
        DailyParams(
            audio_out_enabled=True,
            audio_out_sample_rate=tts.sample_rate,
            transcription_enabled=False,
            vad_enabled=True,
            vad_analyzer=SileroVADAnalyzer(params=VADParams(stop_secs=0.3)),
            vad_audio_passthrough=True,
        ),
    )

    conversation = await Conversation.get_conversation_by_id(params.conversation_id, db)
    if not conversation:
        raise Exception(f"Conversation {params.conversation_id} not found")
    messages = [getattr(msg, "content") for msg in conversation.messages]

    tools = NOT_GIVEN  # todo: implement tools in and set here
    context = OpenAILLMContext(messages, tools)
    context_aggregator = llm.create_context_aggregator(context)
    user_aggregator = context_aggregator.user()
    assistant_aggregator = context_aggregator.assistant()

    storage = PersistentContext(context=context)

    #
    # RTVI
    #

    rtvi = await create_rtvi_processor(config, user_aggregator)

    # This will send `user-*-speaking` and `bot-*-speaking` messages.
    rtvi_speaking = RTVISpeakingProcessor()

    # This will send `user-transcription` messages.
    rtvi_user_transcription = RTVIUserTranscriptionProcessor()

    # This will send `bot-transcription` messages.
    rtvi_bot_transcription = RTVIBotTranscriptionProcessor()

    # This will send `bot-llm-*` messages.
    rtvi_bot_llm = RTVIBotLLMProcessor()

    # This will send `bot-tts-*` messages.
    rtvi_bot_tts = RTVIBotTTSProcessor(direction=FrameDirection.UPSTREAM)

    processors = [
        transport.input(),
        rtvi,
        rtvi_speaking,
        stt,
        rtvi_user_transcription,
        user_aggregator,
        storage.create_processor(),
        llm,
        rtvi_bot_llm,
        rtvi_bot_transcription,
        tts,
        transport.output(),
        rtvi_bot_tts,
        assistant_aggregator,
        storage.create_processor(exit_on_endframe=True, push_transport_message_upstream=True),
    ]

    pipeline = Pipeline(processors)

    @storage.on_context_message
    async def on_context_message(messages: list[Any]):
        logger.debug(f"{len(messages)} message(s) received for storage: {messages}")

        try:
            await Message.save_messages(
                str(params.conversation_id),
                conversation.language_code,
                messages,
                db,
            )
        except Exception as e:
            logger.error(f"Error storing messages: {e}")
            raise e

    @rtvi.event_handler("on_client_ready")
    async def on_client_ready(rtvi):
        await rtvi.set_bot_ready()
        for message in params.actions:
            await rtvi.handle_message(message)

    @transport.event_handler("on_first_participant_joined")
    async def on_first_participant_joined(transport, participant):
        await callbacks.on_first_participant_joined(participant)

    @transport.event_handler("on_participant_joined")
    async def on_participant_joined(transport, participant):
        await callbacks.on_participant_joined(participant)

    @transport.event_handler("on_participant_left")
    async def on_participant_left(transport, participant, reason):
        await callbacks.on_participant_left(participant, reason)

    @transport.event_handler("on_call_state_updated")
    async def on_call_state_updated(transport, state):
        await callbacks.on_call_state_updated(state)

    return pipeline
