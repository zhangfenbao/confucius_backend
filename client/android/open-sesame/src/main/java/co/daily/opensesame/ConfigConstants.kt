package co.daily.opensesame

import androidx.compose.runtime.Immutable

object ConfigConstants {

    const val DEFAULT_PROMPT =
        "You are Sesame, a friendly assistant. Keep your responses brief, when possible or not requested differently. Avoid bold and italic text formatting (**bold** and *italic*) in your responses."

    enum class InteractionMode : NamedOption {
        Informational {
            override val id = "informational"
            override val displayName = "Informational"
            override val displayDescription = "Show LLM text output immediately"
        },
        Conversational {
            override val id = "conversational"
            override val displayName = "Conversational"
            override val displayDescription =
                "Show LLM output word-by-word, in time with the TTS output"
        };

        companion object {
            fun fromId(id: String?) = when (id) {
                Informational.id -> Informational
                Conversational.id -> Conversational
                else -> null
            }
        }
    }

    val interactionModes = NamedOptionList(
        listOf(
            InteractionMode.Informational,
            InteractionMode.Conversational
        )
    )

    object Together : LLMProvider {

        val Llama8B =
            "Llama 3.1 8B Instruct Turbo" isLLMModel "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo"
        val Llama70B =
            "Llama 3.1 70B Instruct Turbo" isLLMModel "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo"
        val Llama405B =
            "Llama 3.1 405B Instruct Turbo" isLLMModel "meta-llama/Meta-Llama-3.1-405B-Instruct-Turbo"

        override val displayName = "Together AI"
        override val id = "together"

        override val models =
            NamedOptionList(listOf(Llama8B, Llama70B, Llama405B), default = Llama70B)
    }

    object Anthropic : LLMProvider {

        override val displayName = "Anthropic"
        override val id = "anthropic"

        override val models =
            NamedOptionList(listOf("Claude Sonnet 3.5" isLLMModel "claude-3-5-sonnet-20240620"))
    }

    object OpenAI : LLMProvider {
        override val displayName = "OpenAI"
        override val id = "openai"

        override val models = NamedOptionList(listOf("GPT-4o" isLLMModel "gpt-4o-2024-05-13"))
    }

    object ElevenLabs : TTSProvider {

        override val displayName = "ElevenLabs"
        override val id = "elevenlabs"

        override val languageCodes = LanguageMap(
            english = "en",
            french = "fr",
            german = "de",
            spanish = "es"
        )

        override val model = LanguageMap(all = "eleven_turbo_v2_5")

        override val voices = LanguageMap(
            all = NamedOptionList(
                listOf(
                    "Alice" isVoice "Xb7hH8MSUJpSbSDYk0k2",
                    "Aria" isVoice "9BWtsMINqrJLrRacOk9x",
                    "Bill" isVoice "pqHfZKP75CvOlQylNhV4",
                    "Brian" isVoice "nPczCjzI2devNBz1zQrb",
                    "Callum" isVoice "N2lVS1w4EtoT3dr4eOWO",
                    "Charlie" isVoice "IKne3meq5aSn9XLyUdCD",
                    "Charlotte" isVoice "XB0fDUnXU5powFXDhCwa",
                    "Chris" isVoice "iP95p4xoKVk53GoZ742B",
                    "Daniel" isVoice "onwK4e9ZLuTAKqWW03F9",
                    "Eric" isVoice "cjVigY5qzO86Huf0OWal",
                    "George" isVoice "JBFqnCBsd6RMkjVDRZzb",
                    "Jessica" isVoice "cgSgspJ2msm6clMCkdW9",
                    "Laura" isVoice "FGY2WhTYpPnrIDTdsKH5",
                    "Liam" isVoice "TX3LPaxmHKxFdv7VOQHJ",
                    "Lily" isVoice "pFZP5JQG7iQjIQuC4Bku",
                    "Matilda" isVoice "XrExE9yKIg1WjnnlVkGX",
                    "River" isVoice "SAz9YHcvj6GT2YYXdXww",
                    "Roger" isVoice "CwhRBWXzGAHq8TQ4Fs17",
                    "Sarah" isVoice "EXAVITQu4vr4xnSDxMaL",
                    "Will" isVoice "bIHbv24MWmeRgasZH58o",
                )
            )
        )
    }

    object Cartesia : TTSProvider {

        override val displayName = "Cartesia"
        override val id = "cartesia"

        override val languageCodes = LanguageMap(
            english = "en",
            french = "fr",
            german = "de",
            spanish = "es"
        )

        override val model = LanguageMap(english = "sonic-english", other = "sonic-multilingual")

        override val voices = LanguageMap(
            english = NamedOptionList(
                listOf(
                    "British Lady" isVoice "79a125e8-cd45-4c13-8a67-188112f4dd22",
                    "California Girl" isVoice "b7d50908-b17c-442d-ad8d-810c63997ed9",
                    "Child" isVoice "2ee87190-8f84-4925-97da-e52547f9462c",
                    "Classy British Man" isVoice "95856005-0332-41b0-935f-352e296aa0df",
                    "Confident British Man" isVoice "63ff761f-c1e8-414b-b969-d1833d1c870c",
                    "Doctor Mischief" isVoice "fb26447f-308b-471e-8b00-8e9f04284eb5",
                    "Female Nurse" isVoice "5c42302c-194b-4d0c-ba1a-8cb485c84ab9",
                    "Friendly Reading Man" isVoice "69267136-1bdc-412f-ad78-0caad210fb40",
                    "Helpful Woman" isVoice "156fb8d2-335b-4950-9cb3-a2d33befec77",
                    "Kentucky Man" isVoice "726d5ae5-055f-4c3d-8355-d9677de68937",
                    "Madame Mischief" isVoice "e13cae5c-ec59-4f71-b0a6-266df3c9bb8e",
                    "Movieman" isVoice "c45bc5ec-dc68-4feb-8829-6e6b2748095d",
                    "Newsman" isVoice "d46abd1d-2d02-43e8-819f-51fb652c1c61",
                    "Polite Man" isVoice "ee7ea9f8-c0c1-498c-9279-764d6b56d189",
                    "Salesman" isVoice "820a3788-2b37-4d21-847a-b65d8a68c99a",
                    "Southern Woman" isVoice "f9836c6e-a0bd-460e-9d3c-f7299fa60f94",
                    "Storyteller Lady" isVoice "996a8b96-4804-46f0-8e05-3fd4ef1a87cd",
                    "The Merchant" isVoice "50d6beb4-80ea-4802-8387-6c948fe84208",
                )
            ),
            french = NamedOptionList(
                listOf(
                    "Calm French Woman" isVoice "a8a1eb38-5f15-4c1d-8722-7ac0f329727d",
                    "French Conversational Lady" isVoice "a249eaff-1e96-4d2c-b23b-12efa4f66f41",
                    "French Narrator Lady" isVoice "8832a0b5-47b2-4751-bb22-6a8e2149303d",
                    "French Narrator Man" isVoice "5c3c89e5-535f-43ef-b14d-f8ffe148c1f0",
                    "Friendly French Man" isVoice "ab7c61f5-3daa-47dd-a23b-4ac0aac5f5c3",
                    "Helpful French Lady" isVoice "65b25c5d-ff07-4687-a04c-da2f43ef6fa9",
                    "Stern French Man" isVoice "0418348a-0ca2-4e90-9986-800fb8b3bbc0",
                )
            ),
            german = NamedOptionList(
                listOf(
                    "Friendly German Man" isVoice "fb9fcab6-aba5-49ec-8d7e-3f1100296dde",
                    "German Conversation Man" isVoice "384b625b-da5d-49e8-a76d-a2855d4f31eb",
                    "German Conversational Woman" isVoice "3f4ade23-6eb4-4279-ab05-6a144947c4d5",
                    "German Reporter Man" isVoice "3f6e78a8-5283-42aa-b5e7-af82e8bb310c",
                    "German Reporter Woman" isVoice "119e03e4-0705-43c9-b3ac-a658ce2b6639",
                    "German Storyteller Man" isVoice "db229dfe-f5de-4be4-91fd-7b077c158578",
                    "German Woman" isVoice "b9de4a89-2257-424b-94c2-db18ba68c81a",
                )
            ),
            spanish = NamedOptionList(
                listOf(
                    "Mexican Man" isVoice "15d0c2e2-8d29-44c3-be23-d585d5f154a1",
                    "Mexican Woman" isVoice "5c5ad5e7-1020-476b-8b91-fdcbe9cc313c",
                    "Spanish Narrator Lady" isVoice "2deb3edf-b9d8-4d06-8db9-5742fb8a3cb2",
                    "Spanish Narrator Man" isVoice "a67e0421-22e0-4d5b-b586-bd4a64aee41d",
                    "Spanish Speaking Lady" isVoice "846d6cb0-2301-48b6-9683-48f5618ea2f6",
                    "Spanish Speaking Man" isVoice "34dbb662-8e98-413c-a1ef-1a3407675fe7",
                    "Spanish Speaking Reporter Man" isVoice "2695b6b5-5543-4be1-96d9-3967fb5e7fec",
                    "Young Spanish Speaking Woman" isVoice "db832ebd-3cb6-42e7-9d47-912b425adbaa",
                    "Spanish Speaking Storyteller Man" isVoice "846fa30b-6e1a-49b9-b7df-6be47092a09a",
                )
            )
        )
    }

    object Deepgram : STTProvider {
        override val displayName = "Deepgram"
        override val id = "deepgram"

        override val languageCodes = LanguageMap(
            english = "en",
            french = "fr",
            german = "de",
            spanish = "es"
        )

        override val model = LanguageMap(
            english = "nova-2-conversationalai",
            other = "nova-2-general"
        )
    }

    val llmProviders = NamedOptionList(listOf(Anthropic, OpenAI, Together), default = Together)
    val ttsProviders = NamedOptionList(listOf(Cartesia, ElevenLabs))
    val sttProviders = NamedOptionList<STTProvider>(listOf(Deepgram))

    val languages = NamedOptionList(
        options = listOf(
            Language.English,
            Language.French,
            Language.German,
            Language.Spanish
        ), default = Language.English
    )

    val workspaceLanguageId = LanguageMap(
        english = "english",
        french = "french",
        german = "german",
        spanish = "spanish"
    )

    val llmModelNamesById: Map<String, String> =
        llmProviders.options.flatMap { it.models.options }
            .associate { it.id to it.displayName }
}

data class LanguageMap<E>(
    val english: E,
    val french: E,
    val german: E,
    val spanish: E
) {
    constructor(english: E, other: E) : this(
        english = english,
        french = other,
        german = other,
        spanish = other
    )

    constructor(all: E) : this(all, all)

    fun get(language: Language) = when (language) {
        Language.English -> english
        Language.French -> french
        Language.German -> german
        Language.Spanish -> spanish
    }
}

sealed class Language(
    override val id: String,
    override val displayName: String
) : NamedOption {
    data object English : Language("english", "English")
    data object French : Language("french", "French")
    data object German : Language("german", "German")
    data object Spanish : Language("spanish", "Spanish")
}

@Immutable
data class NamedOptionList<E : NamedOption>(
    val options: List<E>,
    val default: E = options.first()
) {
    fun byIdOrDefault(id: String?) =
        id?.let { idNonNull -> options.firstOrNull { it.id == idNonNull } } ?: default
}

interface NamedOption {
    val displayName: String

    val displayDescription: String?
        get() = null

    val id: String
}

interface STTProvider : NamedOption {
    override val displayName: String
    override val id: String
    val languageCodes: LanguageMap<String>
    val model: LanguageMap<String>
}

interface TTSProvider : NamedOption {
    override val displayName: String
    override val id: String
    val languageCodes: LanguageMap<String>
    val model: LanguageMap<String>
    val voices: LanguageMap<NamedOptionList<TTSOptionVoice>>
}

interface LLMProvider : NamedOption {
    override val displayName: String
    override val id: String
    val models: NamedOptionList<LLMOptionModel>
}

data class LLMOptionModel(
    override val displayName: String,
    override val id: String,
) : NamedOption

data class TTSOptionVoice(
    override val displayName: String,
    override val id: String,
) : NamedOption

private infix fun String.isLLMModel(id: String) = LLMOptionModel(displayName = this, id = id)
private infix fun String.isVoice(id: String) = TTSOptionVoice(displayName = this, id = id)