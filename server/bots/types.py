from typing import Any, Awaitable, Callable, List, Mapping, Optional

from pydantic import BaseModel

from pipecat.processors.frameworks.rtvi import RTVIMessage, RTVIServiceConfig


class BotConfig(BaseModel):
    services: Mapping[str, str] = {}
    api_keys: Mapping[str, str] = {}
    daily_api_key: str = ""
    config: List[RTVIServiceConfig] = []
    service_options: Mapping[str, Mapping[str, Any]] = {}


class BotParams(BaseModel):
    conversation_id: str
    workspace_id: Optional[str] = None
    actions: List[RTVIMessage] = []


class BotCallbacks(BaseModel):
    on_call_state_updated: Callable[[str], Awaitable[None]]
    on_first_participant_joined: Callable[[Mapping[str, Any]], Awaitable[None]]
    on_participant_joined: Callable[[Mapping[str, Any]], Awaitable[None]]
    on_participant_left: Callable[[Mapping[str, Any], str], Awaitable[None]]
