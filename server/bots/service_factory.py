import os
from typing import Any, Mapping

from pipecat.services.ai_services import AIService
from pipecat.services.anthropic import AnthropicLLMService
from pipecat.services.azure import AzureTTSService
from pipecat.services.cartesia import CartesiaTTSService
from pipecat.services.deepgram import DeepgramSTTService
from pipecat.services.elevenlabs import ElevenLabsTTSService
from pipecat.services.openai import OpenAILLMService, OpenAITTSService
from pipecat.services.playht import PlayHTTTSService
from pipecat.services.together import TogetherLLMService
from pipecat.utils.text.markdown_text_filter import MarkdownTextFilter


def service_factory_get(
    service: str,
    api_keys: Mapping[str, str],
    service_options: Mapping[str, Mapping[str, Any]],
) -> AIService:
    match service:
        # STT cases
        case "deepgram":
            return DeepgramSTTService(
                api_key=api_keys.get(service, os.getenv("DEEPGRAM_API_KEY", ""))
            )
        # LLM cases
        case "anthropic":
            return AnthropicLLMService(
                api_key=api_keys.get(service, os.getenv("ANTHROPIC_API_KEY", ""))
            )
        case "together":
            return TogetherLLMService(
                api_key=api_keys.get(service, os.getenv("TOGETHER_API_KEY", ""))
            )
        case "openai":
            return OpenAILLMService(api_key=api_keys.get(service, os.getenv("OPENAI_API_KEY", "")))
        case "groq":
            return OpenAILLMService(
                api_key=api_keys.get(service, os.getenv("GROQ_API_KEY", "")),
                base_url="https://api.groq.com/openai/v1",
            )
        case "custom_llm":
            return OpenAILLMService(
                api_key=api_keys.get(service, ""),
                base_url=service_options.get(service, {}).get("base_url", ""),
            )
        # TTS cases
        case "cartesia":
            return CartesiaTTSService(
                api_key=api_keys.get(service, os.getenv("CARTESIA_API_KEY", "")),
                voice_id="79a125e8-cd45-4c13-8a67-188112f4dd22",
                text_filter=MarkdownTextFilter(),
            )
        case "elevenlabs":
            return ElevenLabsTTSService(
                api_key=api_keys.get(service, os.getenv("ELEVENLABS_API_KEY", "")),
                voice_id="pFZP5JQG7iQjIQuC4Bku",
                text_filter=MarkdownTextFilter(),
            )
        case "playht":
            return PlayHTTTSService(
                api_key=api_keys.get(service, os.getenv("PLAYHT_API_KEY", "")),
                user_id=service_options.get(service, {}).get("user_id", ""),
                voice_url="s3://voice-cloning-zero-shot/820da3d2-3a3b-42e7-844d-e68db835a206/sarah/manifest.json",
                text_filter=MarkdownTextFilter(),
            )
        case "azure_tts":
            return AzureTTSService(
                api_key=api_keys.get(service, os.getenv("AZURE_API_KEY", "")),
                region=service_options.get(service, {}).get("region", ""),
                voice_id="en-US-SaraNeural",
                text_filter=MarkdownTextFilter(),
            )
        case "openai_tts":
            return OpenAITTSService(
                api_key=api_keys.get(service, os.getenv("OPENAI_API_KEY", "")),
                voice_id="nova",
                sample_rate=service_options.get(service, {}).get("sample_rate", 24000),
                text_filter=MarkdownTextFilter(),
            )

    raise Exception(f"Service '{service}' is not available")
