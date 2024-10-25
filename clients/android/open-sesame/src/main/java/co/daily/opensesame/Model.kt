package co.daily.opensesame

import ai.rtvi.client.types.ServiceConfig
import ai.rtvi.client.types.Value
import androidx.compose.runtime.Immutable
import co.daily.opensesame.utils.MutableJsonElement
import co.daily.opensesame.utils.Timestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@JvmInline
@Immutable
@Serializable
value class WorkspaceId(val id: String)

@JvmInline
@Immutable
@Serializable
value class ConversationId(val id: String)

@Immutable
sealed interface MessageId {

    @Immutable
    data class Permanent(override val key: String) : MessageId

    @Immutable
    data class Temporary(override val key: String) : MessageId

    val key: String
}

@Immutable
@Serializable
data class Workspace(
    @SerialName("workspace_id")
    val id: WorkspaceId,
    val title: String? = null,
    val config: JsonElement? = null,
    @SerialName("created_at")
    val createdAt: Timestamp,
    @SerialName("updated_at")
    val updatedAt: Timestamp
) {
    fun getConfigOption(service: String, name: String): JsonElement? {

        return config?.getArray("config")
            ?.getElemWithTag("service", service)
            ?.getArray("options")
            ?.getElemWithTag("name", name)
            ?.getProperty("value")
    }

    fun getConfigOptionString(service: String, name: String) =
        (getConfigOption(service, name) as? JsonPrimitive)?.contentOrNull
}

private fun JsonElement.getProperty(key: String) = (this as? JsonObject)?.get(key)
private fun JsonElement.getObject(key: String) = getProperty(key) as? JsonObject
private fun JsonElement.getArray(key: String) = getProperty(key) as? JsonArray
private fun JsonElement.getString(key: String) = (getProperty(key) as? JsonPrimitive)?.contentOrNull

private fun JsonArray.getElemWithTag(tagName: String, tagValue: String) =
    firstOrNull { it.getString(tagName) == tagValue }

@Immutable
@Serializable
data class Conversation(
    @SerialName("conversation_id")
    val conversationId: ConversationId,
    @SerialName("workspace_id")
    val workspaceId: WorkspaceId,
    val title: String? = null,
    val archived: Boolean = false,
    @SerialName("language_code")
    val languageCode: String? = null,
    @SerialName("created_at")
    val createdAt: Timestamp,
    @SerialName("updated_at")
    val updatedAt: Timestamp
)

@Immutable
data class Message(
    val id: MessageId,
    val role: MessageRole,
    val stored: Boolean,
    val contentFinal: String,
    val contentPending: String,
) {
    companion object {
        fun fromRaw(msg: RawMessage) = Message(
            id = MessageId.Permanent(msg.messageId),
            role = when (msg.content.role) {
                "system" -> MessageRole.System
                "assistant" -> MessageRole.Assistant
                else -> MessageRole.User
            },
            contentFinal = (msg.content.content as? Value.Str)?.value
                ?: msg.content.content.toString(),
            contentPending = "",
            stored = true,
        )
    }
}

@Immutable
@Serializable
data class RawMessage(
    @SerialName("message_id")
    val messageId: String,
    val content: Content,
    @SerialName("created_at")
    val createdAt: Timestamp,
    @SerialName("updated_at")
    val updatedAt: Timestamp
) {
    @Serializable
    data class Content(
        val role: String,
        val content: Value
    )
}

@Immutable
enum class MessageRole {
    User,
    Assistant,
    System;

    companion object {
        fun fromString(value: String?) = when (value) {
            "assistant" -> Assistant
            "system" -> System
            "user" -> User
            else -> null
        }
    }
}

// Note: this structure could be lossy during deserialization, as the config may contain
// fields we're not aware of.
@Immutable
@Serializable
data class WorkspaceConfigRaw(
    val config: List<ServiceConfig>? = null,
    @SerialName("api_keys")
    val apiKeys: Map<String, String?>? = null,
    @SerialName("services")
    val services: Map<String, String?>? = null,
    @SerialName("default_llm_context")
    val defaultLlmContext: List<LLMContextElement>,
    @SerialName("interaction_mode")
    val interactionMode: String? = null

) {
    fun getOption(service: String, optionName: String) =
        config?.firstOrNull { it.service == service }?.options?.firstOrNull { it.name == optionName }?.value

    fun getOptionString(service: String, optionName: String) =
        (getOption(service, optionName) as? Value.Str)?.value
}

@Immutable
@Serializable
data class LLMContextElement(
    val content: Content,
    @SerialName("extra_metadata")
    val extraMetadata: Value? = null
) {
    @Immutable
    @Serializable
    data class Content(
        val role: String,
        val content: Value
    )
}

@Immutable
data class WorkspaceConfig(
    val title: String,
    val apiKeys: Map<String, String>,
    val language: Language,
    val interactionMode: ConfigConstants.InteractionMode,
    val readMarkdownBlocks: Boolean,
    val sttProvider: STTProvider,
    val llmProvider: LLMProvider,
    val llmModel: LLMOptionModel,
    val ttsProvider: TTSProvider,
    val ttsVoice: TTSOptionVoice,
    val prompt: List<LLMContextElement>
) {
    companion object {
        private val JSON_INSTANCE = Json { ignoreUnknownKeys = true }

        val Default = WorkspaceConfig(
            title = "",
            apiKeys = emptyMap(),
            language = Language.English,
            interactionMode = ConfigConstants.interactionModes.default,
            readMarkdownBlocks = false,
            sttProvider = ConfigConstants.Deepgram,
            llmProvider = ConfigConstants.Together,
            llmModel = ConfigConstants.Together.models.default,
            ttsProvider = ConfigConstants.Cartesia,
            ttsVoice = ConfigConstants.Cartesia.voices.english.default,
            prompt = listOf(
                LLMContextElement(
                    content = LLMContextElement.Content(
                        role = "system",
                        content = Value.Str(ConfigConstants.DEFAULT_PROMPT)
                    )
                )
            )
        )

        private fun parseRawLanguage(value: String?) = when (value) {
            "en", "english" -> Language.English
            "fr", "french" -> Language.French
            "de", "german" -> Language.German
            "es", "spanish" -> Language.Spanish
            else -> null
        }

        fun fromJsonConfig(
            title: String,
            value: JsonElement
        ): WorkspaceConfig {

            val rawConfig = JSON_INSTANCE.decodeFromJsonElement<WorkspaceConfigRaw>(value)

            val services = rawConfig.services

            val language = parseRawLanguage(rawConfig.getOptionString("tts", "language"))
                ?: parseRawLanguage(rawConfig.getOptionString("stt", "language"))
                ?: Language.English

            val sttProvider = ConfigConstants.sttProviders.byIdOrDefault(services?.get("stt"))
            val llmProvider = ConfigConstants.llmProviders.byIdOrDefault(services?.get("llm"))
            val ttsProvider = ConfigConstants.ttsProviders.byIdOrDefault(services?.get("tts"))

            val textFilter = (rawConfig.getOption("tts", "text_filter") as? Value.Object)
                ?.value
                ?.let {
                    it["filter_code"] == Value.Bool(true)
                            || it["filter_tables"] == Value.Bool(true)
                } ?: false

            return WorkspaceConfig(
                title = title,
                apiKeys = rawConfig.apiKeys?.mapNotNull { (key, value) -> value?.let { key to it } }
                    ?.toMap()
                    ?: emptyMap(),
                language = language,
                interactionMode = ConfigConstants.interactionModes.byIdOrDefault(rawConfig.interactionMode),
                sttProvider = sttProvider,
                llmProvider = llmProvider,
                llmModel = llmProvider.models.byIdOrDefault(
                    rawConfig.getOptionString(
                        "llm",
                        "model"
                    )
                ),
                ttsProvider = ttsProvider,
                ttsVoice = ttsProvider.voices.get(language).byIdOrDefault(
                    rawConfig.getOptionString(
                        "tts",
                        "voice"
                    )
                ),
                prompt = rawConfig.defaultLlmContext,
                readMarkdownBlocks = !textFilter
            )
        }
    }

    fun asNewJsonConfig() = MutableJsonElement.Object().also(::updateJsonConfig)

    fun updateJsonConfig(value: MutableJsonElement.Object) {
        value.set("api_keys", apiKeys)

        value.set(
            "services", mapOf(
                "llm" to llmProvider.id,
                "stt" to sttProvider.id,
                "tts" to ttsProvider.id
            )
        )

        value.set(
            "default_llm_context",
            prompt.map {
                JSON_INSTANCE.decodeFromJsonElement<MutableJsonElement>(
                    JSON_INSTANCE.encodeToJsonElement(it)
                )
            })

        value.set("interaction_mode", interactionMode.id)

        fun setOption(service: String, optionName: String, optionValue: MutableJsonElement) {
            value.getOrAddArray("config")
                .getOrAddElemWithTag("service", service)
                .getOrAddArray("options")
                .getOrAddElemWithTag("name", optionName)
                .set("value", optionValue)
        }

        fun setOption(service: String, optionName: String, optionValue: String) {
            setOption(service, optionName, MutableJsonElement.Str(optionValue))
        }

        setOption(service = "llm", optionName = "model", optionValue = llmModel.id)

        setOption(service = "tts", optionName = "voice", optionValue = ttsVoice.id)
        setOption(
            service = "tts",
            optionName = "model",
            optionValue = ttsProvider.model.get(language)
        )
        setOption(
            service = "tts",
            optionName = "language",
            optionValue = ttsProvider.languageCodes.get(language)
        )

        setOption(
            service = "stt",
            optionName = "model",
            optionValue = sttProvider.model.get(language)
        )
        setOption(
            service = "stt",
            optionName = "model",
            optionValue = sttProvider.model.get(language)
        )
        setOption(
            service = "stt",
            optionName = "language",
            optionValue = sttProvider.languageCodes.get(language)
        )

        setOption(
            service = "tts",
            optionName = "text_filter",
            optionValue = MutableJsonElement.Object(
                "filter_code" to MutableJsonElement.Bool(!readMarkdownBlocks),
                "filter_tables" to MutableJsonElement.Bool(!readMarkdownBlocks),
            )
        )
    }
}
