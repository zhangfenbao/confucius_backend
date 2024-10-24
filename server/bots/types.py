from typing import Any, Awaitable, Callable, List, Mapping

from pipecat.processors.frameworks.rtvi import RTVIMessage, RTVIServiceConfig
from pydantic import BaseModel


class BotConfig(BaseModel):
    services: Mapping[str, str] = {}
    config: List[RTVIServiceConfig] = []


class BotParams(BaseModel):
    conversation_id: str
    actions: List[RTVIMessage] = []


class BotCallbacks(BaseModel):
    on_call_state_updated: Callable[[str], Awaitable[None]]
    on_first_participant_joined: Callable[[Mapping[str, Any]], Awaitable[None]]
    on_participant_joined: Callable[[Mapping[str, Any]], Awaitable[None]]
    on_participant_left: Callable[[Mapping[str, Any], str], Awaitable[None]]
