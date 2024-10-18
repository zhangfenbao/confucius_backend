package co.daily.opensesame.chat

import ai.rtvi.client.transport.MsgServerToClient
import ai.rtvi.client.types.Transcript
import androidx.compose.runtime.Immutable
import co.daily.opensesame.MessageRole

@Immutable
interface ChatMessageProcessorHelper {

    fun appendOrCreateMessage(
        role: MessageRole,
        text: String,
        transcriptionFinal: Boolean
    )

    fun finalizeMessage(
        role: MessageRole,
        replacementText: String?
    )
}

@Immutable
interface ChatMessageProcessor {

    fun onBotTTSStarted() {}

    fun onBotLLMStarted() {}

    fun onUserStartedSpeaking() {}

    fun onUserTranscript(data: Transcript) {}

    fun onBotLLMText(data: MsgServerToClient.Data.BotLLMTextData) {}

    fun onBotTTSText(data: MsgServerToClient.Data.BotTTSTextData) {}

    fun onStorageItemStored(content: String, role: MessageRole) {}
}