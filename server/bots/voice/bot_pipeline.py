from typing import cast

from bots.context_storage import PersistentContextStorage
from bots.rtvi import create_rtvi_processor
from bots.service_factory import service_factory_get
from bots.types import BotCallbacks, BotConfig, BotParams
from common.models import Conversation
from openai._types import NOT_GIVEN
from pipecat.pipeline.pipeline import Pipeline
from pipecat.processors.frame_processor import FrameDirection
from pipecat.processors.frameworks.rtvi import (
    RTVIBotLLMProcessor,
    RTVIBotTTSProcessor,
    RTVISpeakingProcessor,
    RTVIUserTranscriptionProcessor,
)
from pipecat.services.ai_services import (
    LLMService,
    OpenAILLMContext,
    STTService,
    TTSService,
)
from pipecat.transports.services.daily import DailyParams, DailyTransport
from pipecat.vad.silero import SileroVADAnalyzer
from pipecat.vad.vad_analyzer import VADParams
from sqlalchemy.ext.asyncio import AsyncSession


async def voice_bot_pipeline(
    params: BotParams,
    config: BotConfig,
    callbacks: BotCallbacks,
    room_url: str,
    room_token: str,
    db: AsyncSession,
) -> Pipeline:
    api_keys = config.api_keys
    services = config.services
    service_options = config.service_options

    if "llm" not in services:
        raise Exception("Service `llm` not available in provided services.")
    if "tts" not in services:
        raise Exception("Service `tts` not available in provided services.")
    if "stt" not in services:
        raise Exception("Service `stt` not available in provided services.")

    llm = cast(LLMService, service_factory_get(services["llm"], api_keys, service_options))
    tts = cast(TTSService, service_factory_get(services["tts"], api_keys, service_options))
    stt = cast(STTService, service_factory_get(services["stt"], api_keys, service_options))

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
    messages = [msg.content for msg in conversation.messages]

    tools = NOT_GIVEN  # todo: implement tools in and set here
    context = OpenAILLMContext(messages, tools)
    context_aggregator = llm.create_context_aggregator(context)
    user_aggregator = context_aggregator.user()
    assistant_aggregator = context_aggregator.assistant()

    context_storage = PersistentContextStorage(
        db=db,
        conversation_id=params.conversation_id,
        context=context,
        language_code=conversation.language_code,
    )

    #
    # RTVI
    #

    rtvi = await create_rtvi_processor(config, user_aggregator)

    # This will send `user-*-speaking` and `bot-*-speaking` messages.
    rtvi_speaking = RTVISpeakingProcessor()

    # This will send `user-transcription` messages.
    rtvi_user_transcription = RTVIUserTranscriptionProcessor()

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
        context_storage.create_processor(),
        llm,
        rtvi_bot_llm,
        tts,
        transport.output(),
        rtvi_bot_tts,
        assistant_aggregator,
        context_storage.create_processor(
            exit_on_endframe=True, push_transport_message_upstream=True
        ),
    ]

    pipeline = Pipeline(processors)

    @rtvi.event_handler("on_bot_ready")
    async def on_bot_ready(rtvi):
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
