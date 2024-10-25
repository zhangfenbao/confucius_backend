package co.daily.opensesame

import ai.rtvi.client.RTVIClientOptions
import ai.rtvi.client.RTVIClientParams
import ai.rtvi.client.RTVIEventCallbacks
import ai.rtvi.client.daily.DailyVoiceClient
import ai.rtvi.client.result.Future
import ai.rtvi.client.result.RTVIError
import ai.rtvi.client.result.resolvedPromiseErr
import ai.rtvi.client.transport.MsgServerToClient
import ai.rtvi.client.types.Option
import ai.rtvi.client.types.Participant
import ai.rtvi.client.types.Transcript
import ai.rtvi.client.types.TransportState
import ai.rtvi.client.types.Value
import ai.rtvi.client.utils.ThreadRef
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import co.daily.opensesame.chat.ChatMessageProcessor

private const val TAG = "ComposableVoiceClient"

enum class VoiceClientSessionState {
    Connecting,
    Connected,
    Disconnected
}

@Immutable
class VoiceClientError(
    val message: String,
    val detail: String? = null
)

@Stable
sealed interface ClientState

@Stable
class TextClientState : ClientState {
    private val appendMessageCallback =
        mutableStateOf<((String) -> Future<Value, RTVIError>)?>(null)

    fun internalSetAppendMessageCallback(callback: (String) -> Future<Value, RTVIError>) {
        appendMessageCallback.value = callback
    }

    fun appendMessage(text: String): Future<Value, RTVIError> {
        return appendMessageCallback.value?.invoke(text) ?: resolvedPromiseErr(
            ThreadRef.forMain(),
            RTVIError.OtherError("Invalid state")
        )
    }
}

@Stable
class VoiceClientState : ClientState {
    private val micDesiredState = mutableStateOf(true)
    private val micActualState = mutableStateOf(false)

    private val botState = mutableStateOf(VoiceClientSessionState.Connecting)

    private val desiredMicStateCallback = mutableStateOf<((Boolean) -> Unit)?>(null)

    private val audioLevelUser = mutableFloatStateOf(0f)
    private val audioLevelBot = mutableFloatStateOf(0f)
    private val isTalkingUser = mutableStateOf(false)
    private val isTalkingBot = mutableStateOf(false)

    val state: VoiceClientSessionState
        get() = botState.value

    val isMicEnabled: Boolean
        get() = micActualState.value

    val desiredMicState: Boolean
        get() = micDesiredState.value

    val userAudioLevel: FloatState
        get() = audioLevelUser

    val botAudioLevel: FloatState
        get() = audioLevelBot

    val userIsTalking: Boolean
        get() = isTalkingUser.value

    val botIsTalking: Boolean
        get() = isTalkingBot.value

    fun setMicEnabled(enabled: Boolean) {
        micDesiredState.value = enabled
        desiredMicStateCallback.value?.invoke(enabled)
    }

    fun internalSetDesiredMicStateCallback(callback: (Boolean) -> Unit) {
        desiredMicStateCallback.value = callback
    }

    fun internalSetMicActualState(enabled: Boolean) {
        micActualState.value = enabled
    }

    fun internalSetBotState(state: VoiceClientSessionState) {
        botState.value = state
    }

    fun internalSetUserAudioLevel(value: Float) {
        audioLevelUser.floatValue = value
    }

    fun internalSetBotAudioLevel(value: Float) {
        audioLevelBot.floatValue = value
    }

    fun internalSetUserIsTalking(value: Boolean) {
        isTalkingUser.value = value
    }

    fun internalSetBotIsTalking(value: Boolean) {
        isTalkingBot.value = value
    }
}

@Composable
fun rememberVoiceClientState() = remember { VoiceClientState() }

@Composable
fun rememberTextClientState() = remember { TextClientState() }

@Composable
fun ComposableVoiceClient(
    onError: (VoiceClientError) -> Unit,
    chatMessageProcessor: ChatMessageProcessor,
    clientState: ClientState,
    conversationId: ConversationId,
    workspaceId: WorkspaceId,
) {
    val context = LocalContext.current

    val baseUrl = Preferences.backendUrl.value
    val apiKey = Preferences.apiKey.value

    val voiceClientState = clientState as? VoiceClientState

    DisposableEffect(baseUrl, apiKey, clientState, conversationId, workspaceId) {

        if (baseUrl == null || apiKey == null) {
            onError(VoiceClientError("API URL and key must be set"))
            return@DisposableEffect onDispose {
                // Nothing to dispose of
            }
        }

        val callbacks = object : RTVIEventCallbacks() {
            override fun onBackendError(message: String) {
                onError(VoiceClientError(message = "Server error", detail = message))
            }

            override fun onTransportStateChanged(state: TransportState) {
                if (clientState is VoiceClientState) {
                    when (state) {
                        TransportState.Ready -> clientState.internalSetBotState(
                            VoiceClientSessionState.Connected
                        )

                        TransportState.Disconnected -> clientState.internalSetBotState(
                            VoiceClientSessionState.Disconnected
                        )

                        else -> {}
                    }
                }
            }

            override fun onUserTranscript(data: Transcript) {
                chatMessageProcessor.onUserTranscript(data)
            }

            override fun onBotTTSText(data: MsgServerToClient.Data.BotTTSTextData) {
                chatMessageProcessor.onBotTTSText(data)
            }

            override fun onBotLLMText(data: MsgServerToClient.Data.BotLLMTextData) {
                chatMessageProcessor.onBotLLMText(data)
            }

            override fun onStorageItemStored(data: MsgServerToClient.Data.StorageItemStoredData) {

                (data.items as? Value.Array)?.value?.forEach { item ->
                    fun getStr(name: String) =
                        ((item as? Value.Object)?.value
                            ?.get(name) as? Value.Str)?.value

                    val content = getStr("content")
                    val role = MessageRole.fromString(getStr("role"))

                    if (content != null && role != null) {
                        chatMessageProcessor.onStorageItemStored(content = content, role = role)
                    }
                }
            }

            override fun onUserAudioLevel(level: Float) {
                voiceClientState?.internalSetUserAudioLevel(level)
            }

            override fun onRemoteAudioLevel(level: Float, participant: Participant) {
                voiceClientState?.internalSetBotAudioLevel(level)
            }

            override fun onBotStartedSpeaking() {
                voiceClientState?.internalSetBotIsTalking(true)
            }

            override fun onBotStoppedSpeaking() {
                voiceClientState?.internalSetBotIsTalking(false)
            }

            override fun onBotLLMStarted() {
                chatMessageProcessor.onBotLLMStarted()
            }

            override fun onBotTTSStarted() {
                chatMessageProcessor.onBotTTSStarted()
            }

            override fun onUserStartedSpeaking() {
                voiceClientState?.internalSetUserIsTalking(true)
                chatMessageProcessor.onUserStartedSpeaking()
            }

            override fun onUserStoppedSpeaking() {
                voiceClientState?.internalSetUserIsTalking(false)
            }

            override fun onInputsUpdated(camera: Boolean, mic: Boolean) {
                voiceClientState?.internalSetMicActualState(mic)
            }
        }

        val backendUrl = baseUrl + (if (baseUrl.endsWith("/")) "" else "/") + "api/rtvi"

        val options = RTVIClientOptions(
            params = RTVIClientParams(
                baseUrl = backendUrl,
                headers = apiKey
                    .takeUnless { it.isEmpty() }
                    ?.let { listOf("Authorization" to "Bearer $it") }
                    ?: emptyList(),
                requestData = listOf(
                    "conversation_id" to Value.Str(conversationId.id),
                    "workspace_id" to Value.Str(workspaceId.id)
                )
            ),
            enableMic = voiceClientState?.desiredMicState ?: false,
            enableCam = false,
        )

        Log.i(TAG, "Voice backend url: $backendUrl")

        val client = DailyVoiceClient(
            context = context,
            callbacks = callbacks,
            options = options
        )

        if (voiceClientState != null) {
            client.connect().withErrorCallback { error ->
                if (error !is RTVIError.OperationCancelled) {
                    onError(VoiceClientError("Connection error", detail = error.description))
                }
            }

            voiceClientState.internalSetDesiredMicStateCallback {
                client.enableMic(it)
            }
        }

        (clientState as? TextClientState)?.internalSetAppendMessageCallback { message ->

            client.action(
                service = "llm",
                action = "append_to_messages",
                arguments = listOf(
                    Option(
                        name = "messages",
                        value = Value.Array(
                            Value.Object(
                                "role" to Value.Str("user"),
                                "content" to Value.Str(message)
                            )
                        )
                    )
                )
            )
        }

        onDispose {
            Log.i(TAG, "Destroying voice client")
            client.enableMic(false)
            if (clientState is VoiceClientState) {
                client.disconnect().withCallback {
                    client.release()
                }
            }
        }
    }

}