package co.daily.opensesame.chat

import ai.rtvi.client.transport.MsgServerToClient
import androidx.compose.runtime.Immutable
import co.daily.opensesame.MessageRole

@Immutable
class ChatMessageProcessorTextMode(
    private val helper: ChatMessageProcessorHelper
) : ChatMessageProcessor {

    override fun onBotLLMStarted() {
        helper.finalizeMessage(
            role = MessageRole.Assistant,
            replacementText = null,
        )
    }

    override fun onBotLLMText(data: MsgServerToClient.Data.BotLLMTextData) {
        helper.appendOrCreateMessage(
            role = MessageRole.Assistant,
            text = data.text,
            transcriptionFinal = true
        )
    }
}