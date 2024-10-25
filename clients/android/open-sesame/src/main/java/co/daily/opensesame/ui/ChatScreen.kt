package co.daily.opensesame.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.daily.opensesame.ClientState
import co.daily.opensesame.ComposableVoiceClient
import co.daily.opensesame.ConfigConstants
import co.daily.opensesame.ConversationId
import co.daily.opensesame.Message
import co.daily.opensesame.MessageId
import co.daily.opensesame.MessageRole
import co.daily.opensesame.PreviewData
import co.daily.opensesame.R
import co.daily.opensesame.TextClientState
import co.daily.opensesame.VoiceClientError
import co.daily.opensesame.VoiceClientSessionState
import co.daily.opensesame.VoiceClientState
import co.daily.opensesame.WorkspaceId
import co.daily.opensesame.api.RepeatedFetchResultRenderer
import co.daily.opensesame.api.RepeatingFetch
import co.daily.opensesame.api.mainThread
import co.daily.opensesame.api.rememberRepeatingFetchState
import co.daily.opensesame.chat.ChatMessageProcessorHelper
import co.daily.opensesame.chat.ChatMessageProcessorTextMode
import co.daily.opensesame.chat.ChatMessageProcessorVoiceModeConversational
import co.daily.opensesame.chat.ChatMessageProcessorVoiceModeInformational
import co.daily.opensesame.ui.theme.LocalAppTheme
import co.daily.opensesame.ui.theme.StyledText
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.mikepenz.markdown.compose.Markdown
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

enum class ChatMode {
    Text,
    Speech
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen(
    onEditWorkspaceClicked: (WorkspaceId) -> Unit,
    onNavButtonClicked: () -> Unit,
    workspaceId: WorkspaceId,
    conversationId: ConversationId?,
) {
    val theme = LocalAppTheme.current.chat
    val api = LocalDataApi.current

    // TODO show error if refresh fails
    val fetchState =
        rememberRepeatingFetchState<List<Message>>(
            key = conversationId,
            default = emptyList(),
            cacheLookupResult = null
        )

    var chatState by remember { mutableStateOf<ClientState>(TextClientState()) }
    var textCompletionInProgress by remember { mutableIntStateOf(0) }

    if (chatState is TextClientState && conversationId != null && textCompletionInProgress == 0) {
        RepeatingFetch(fetchState) { api.getConversationMessages(conversationId) }
    }

    val errors = remember { mutableStateListOf<VoiceClientError>() }

    val micPermissionState: PermissionState =
        rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap ->

            val isRecordAudioGranted = permissionsMap[Manifest.permission.RECORD_AUDIO] ?: false

            if (isRecordAudioGranted) {
                chatState = VoiceClientState()
            } else {
                errors.add(VoiceClientError("Permission denied"))
            }
        }
    )

    fun List<Message>.replaceAtIndex(index: Int, newValue: Message): List<Message> {
        if (index !in indices) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }
        return mapIndexed { i, existingValue ->
            if (i == index) newValue else existingValue
        }
    }

    if (conversationId != null) {

        val workspaceMode = api.cacheLookupWorkspaceMode(workspaceId)
            ?: ConfigConstants.interactionModes.default

        val chatMessageProcessor = remember(chatState, workspaceMode) {

            fun lastUnstoredMessageIndexFor(role: MessageRole) =
                fetchState.result.indexOfLast { it.role == role }
                    .takeIf { it >= 0 && !fetchState.result[it].stored }

            val chatHelper = object : ChatMessageProcessorHelper {
                override fun appendOrCreateMessage(
                    role: MessageRole,
                    text: String,
                    transcriptionFinal: Boolean
                ) {
                    mainThread.runOnThread {
                        val lastMessageIndex = lastUnstoredMessageIndexFor(role)
                        val lastMessage = lastMessageIndex?.let { fetchState.result[it] }

                        fetchState.result =
                            if (lastMessage != null) {
                                fetchState.result.replaceAtIndex(
                                    lastMessageIndex,
                                    if (transcriptionFinal) {
                                        lastMessage.copy(
                                            contentFinal = lastMessage.contentFinal + text,
                                            contentPending = ""
                                        )
                                    } else {
                                        lastMessage.copy(contentPending = text)
                                    }
                                )

                            } else {
                                fetchState.result + Message(
                                    id = MessageId.Temporary(UUID.randomUUID().toString()),
                                    stored = false,
                                    role = role,
                                    contentFinal = text.takeIf { transcriptionFinal } ?: "",
                                    contentPending = text.takeIf { !transcriptionFinal } ?: "",
                                )
                            }
                    }
                }

                override fun finalizeMessage(role: MessageRole, replacementText: String?) {
                    mainThread.runOnThread {

                        val lastMessageIndex = lastUnstoredMessageIndexFor(role)
                        val lastMessage = lastMessageIndex?.let { fetchState.result[it] }

                        val content = replacementText
                            ?: lastMessage?.contentFinal
                            ?: return@runOnThread

                        fetchState.result =
                            if (lastMessage != null) {
                                fetchState.result.replaceAtIndex(
                                    lastMessageIndex,
                                    lastMessage.copy(
                                        contentFinal = content,
                                        contentPending = "",
                                        stored = true
                                    )
                                )

                            } else {
                                fetchState.result + Message(
                                    id = MessageId.Temporary(UUID.randomUUID().toString()),
                                    stored = true,
                                    role = role,
                                    contentFinal = content,
                                    contentPending = ""
                                )
                            }
                    }
                }
            }

            if (chatState is VoiceClientState) {
                when (workspaceMode) {
                    ConfigConstants.InteractionMode.Informational ->
                        ChatMessageProcessorVoiceModeInformational(chatHelper)

                    ConfigConstants.InteractionMode.Conversational ->
                        ChatMessageProcessorVoiceModeConversational(chatHelper)
                }
            } else {
                ChatMessageProcessorTextMode(chatHelper)
            }
        }

        ComposableVoiceClient(
            onError = {
                errors.add(it)
                chatState = TextClientState()
            },
            chatMessageProcessor = chatMessageProcessor,
            clientState = chatState,
            conversationId = conversationId,
            workspaceId = workspaceId
        )
    }

    val voiceState = chatState as? VoiceClientState
    var loading by remember { mutableStateOf(false) }
    var showTitleEditDialog by remember { mutableStateOf(false) }

    val conversationTitle =
        conversationId?.let { api.cacheLookupChatName(it) }?.takeUnless { it.isBlank() }

    if (loading) {
        LoadingDialog()
    }

    if (showTitleEditDialog && conversationId != null) {
        TextInputDialogSave(
            onSave = { newTitle ->
                showTitleEditDialog = false
                loading = true
                api.updateConversation(
                    conversationId = conversationId,
                    newTitle = newTitle,
                ).withCallback {
                    loading = false
                }.withErrorCallback { error ->
                    errors.add(VoiceClientError(error.description))
                }
            },
            onDismissRequest = { showTitleEditDialog = false },
            title = "Conversation title",
            initialValue = conversationTitle ?: ""
        )
    }

    Column(Modifier.fillMaxSize()) {

        ActionBar(
            onNavButtonClicked = onNavButtonClicked,
            onSettingsButtonClicked = { onEditWorkspaceClicked(workspaceId) },
            onEditButtonClicked = conversationId?.let {
                { showTitleEditDialog = true }
            },
            title = if (conversationId != null) {
                conversationTitle ?: "Untitled"
            } else {
                api.cacheLookupWorkspaceName(workspaceId) ?: "Unnamed workspace"
            },
        )

        if (conversationId == null) {
            PlaceholderText("No chat selected")

        } else {
            RepeatedFetchResultRenderer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = fetchState,
                loadingSpinnerColor = theme.foreground
            ) { messages ->
                ChatMessageList(
                    messages = ImmutableMarker(messages),
                    botAudioLevel = voiceState?.botAudioLevel
                )
            }
        }

        AnimatedContent(
            modifier = Modifier.fillMaxWidth(),
            targetState = errors.firstOrNull(),
            transitionSpec = {
                expandVertically(expandFrom = Alignment.Top) togetherWith
                        shrinkVertically(shrinkTowards = Alignment.Top)
            }
        ) { error ->

            if (error == null) {
                Spacer(Modifier.height(0.dp))
            } else {
                val msg = if (error.detail != null) {
                    "${error.message}: ${error.detail}"
                } else {
                    error.message
                }

                StatusBar(
                    text = msg,
                    background = theme.statusBackgroundError,
                    onDismiss = errors::removeFirst,
                    dismissContentDescription = "Dismiss"
                )
            }
        }

        AnimatedContent(
            modifier = Modifier.fillMaxWidth(),
            targetState = voiceState,
            transitionSpec = {
                expandVertically(expandFrom = Alignment.Top) togetherWith
                        shrinkVertically(shrinkTowards = Alignment.Top)
            }
        ) { voiceClientState ->

            if (voiceClientState == null) {
                Spacer(Modifier.height(0.dp))
                return@AnimatedContent
            }

            val dismiss = { chatState = TextClientState() }

            when (voiceClientState.state) {
                VoiceClientSessionState.Connecting -> StatusBarConnecting(dismiss)
                VoiceClientSessionState.Connected -> StatusBarListening(dismiss)

                VoiceClientSessionState.Disconnected -> StatusBar(
                    text = "Disconnected",
                    background = theme.statusBackgroundNeutral,
                    onDismiss = dismiss,
                    dismissContentDescription = "Close"
                )
            }
        }

        AnimatedVisibility(
            visible = conversationId != null && fetchState.initialFetchSucceeded,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            ChatBottomBar(
                onSetChatMode = {

                    errors.clear()

                    when (it) {
                        ChatMode.Text -> {
                            if (chatState !is TextClientState) {
                                chatState = TextClientState()
                            }
                        }

                        ChatMode.Speech -> {
                            if (!micPermissionState.status.isGranted) {
                                permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                            } else {
                                if (chatState !is VoiceClientState) {
                                    chatState = VoiceClientState()
                                }
                            }
                        }
                    }
                },
                onSendMessage = { userMessage ->
                    if (conversationId != null) {
                        (chatState as? TextClientState)?.let { textClientState ->
                            mainThread.runOnThread {
                                fetchState.result += Message(
                                    id = MessageId.Temporary(UUID.randomUUID().toString()),
                                    role = MessageRole.User,
                                    stored = false,
                                    contentFinal = userMessage,
                                    contentPending = ""
                                )
                            }
                            textCompletionInProgress++
                            textClientState.appendMessage(userMessage).withCallback {
                                textCompletionInProgress--
                            }.withErrorCallback { error ->
                                errors.add(VoiceClientError(error.description))
                            }
                        }
                    }
                },
                chatMode = when (chatState) {
                    is TextClientState -> ChatMode.Text
                    is VoiceClientState -> ChatMode.Speech
                },
                userAudioLevel = voiceState?.userAudioLevel?.takeIf { voiceState.userIsTalking }
            )
        }
    }
}

@Composable
private fun ColumnScope.PlaceholderText(text: String) {
    val theme = LocalAppTheme.current.chat

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(64.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        theme.placeholderText.StyledText(text)
    }
}

@Composable
fun ColumnScope.ChatMessageList(
    messages: ImmutableMarker<List<Message>>,
    botAudioLevel: FloatState?
) {
    if (messages.value.isEmpty()) {
        PlaceholderText("No messages yet")

    } else {
        val lazyColumnState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        var previousMessageCount by remember { mutableIntStateOf(0) }

        LaunchedEffect(messages.value.lastOrNull()) {

            val firstScroll = (previousMessageCount == 0)
            previousMessageCount = messages.value.size

            scope.launch {

                suspend fun doScroll() {
                    if (messages.value.isNotEmpty()) {
                        if (firstScroll) {
                            lazyColumnState.scrollToItem(messages.value.size - 1, 100000)
                        } else {
                            lazyColumnState.animateScrollToItem(messages.value.size - 1, 100000)
                        }
                    }
                }

                doScroll()

                // Delay until after the text animation
                delay(1000)

                doScroll()
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            state = lazyColumnState
        ) {
            // Top padding
            item {
                Spacer(Modifier.height(20.dp))
            }

            items(
                items = messages.value,
                key = { it.id.key }
            ) { message ->

                val content = if (message.contentPending.isBlank()) {
                    message.contentFinal
                } else {
                    "${message.contentFinal} ${message.contentPending}"
                }

                when (message.role) {
                    MessageRole.User -> UserChatBubble(text = content)
                    MessageRole.Assistant -> BotChatBubble(
                        text = content,
                        audioLevel = if (message == messages.value.lastOrNull { it.role == MessageRole.Assistant } && message.id is MessageId.Temporary) {
                            botAudioLevel
                        } else {
                            null
                        }
                    )

                    MessageRole.System -> {} // TODO spacing
                }
            }

            // Bottom padding
            item {
                Spacer(Modifier.height(64.dp))
            }
        }
    }
}

@Composable
@Preview
private fun PreviewChatScreen() {
    AppContextPreview {
        ChatScreen(
            conversationId = PreviewData.conversations.values.first().first().conversationId,
            workspaceId = PreviewData.conversations.values.first().first().workspaceId,
            onNavButtonClicked = {},
            onEditWorkspaceClicked = {}
        )
    }
}

@Composable
fun ChatBottomBar(
    onSetChatMode: (ChatMode) -> Unit,
    onSendMessage: (String) -> Unit,
    chatMode: ChatMode,
    userAudioLevel: FloatState?
) {
    val theme = LocalAppTheme.current.chatBottomBar

    Row(
        Modifier
            .fillMaxWidth()
            .background(theme.background)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TODO Currently not supported
        if (false) {
            ToolbarIconButton(
                onClick = { },
                icon = R.drawable.camera,
                contentDescription = "Take photo",
                color = theme.foreground
            )
            ToolbarIconButton(
                onClick = { },
                icon = R.drawable.image,
                contentDescription = "Upload image",
                color = theme.foreground
            )

            Spacer(Modifier.width(8.dp))
        }

        Row(
            Modifier
                .weight(1f)
                .border(1.dp, theme.boxBorder, RoundedCornerShape(25.dp))
                .clip(RoundedCornerShape(25.dp))
                .heightIn(min = 50.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(15.dp))

            val textState = rememberTextFieldState()

            AnimatedContent(
                modifier = Modifier.weight(1f),
                targetState = chatMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { mode ->

                when (mode) {
                    ChatMode.Text -> {
                        BasicTextField(
                            state = textState,
                            textStyle = theme.boxText,
                            cursorBrush = SolidColor(theme.foreground),
                            lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 5),
                            keyboardOptions = KeyboardOptions(
                                autoCorrectEnabled = true,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Send,
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            onKeyboardAction = {
                                onSendMessage(textState.text.toString())
                                textState.clearText()
                            }
                        )

                        if (textState.text.isEmpty()) {
                            theme.boxHintText.StyledText(
                                modifier = Modifier.padding(start = 2.dp),
                                text = "Type message here"
                            )
                        }
                    }

                    ChatMode.Speech -> Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(R.drawable.mic_on),
                            contentDescription = "Microphone",
                            tint = theme.foreground
                        )

                        Spacer(Modifier.width(4.dp))

                        AudioIndicator(
                            modifier = Modifier.size(width = 64.dp, height = 30.dp),
                            level = userAudioLevel,
                            color = theme.foreground
                        )
                    }
                }
            }

            Box(
                Modifier
                    .size(width = 100.dp, height = 38.dp)
                    .clip(RoundedCornerShape(100))
                    .background(theme.toggleOffBackground)
                    .padding(2.dp)
            ) {
                Row {
                    val startSpacing by
                    animateFloatAsState(if (chatMode == ChatMode.Text) 0.000001f else 0.999999f)

                    Spacer(Modifier.weight(startSpacing))

                    Box(
                        Modifier
                            .size(width = 48.dp, height = 34.dp)
                            .clip(RoundedCornerShape(100))
                            .background(theme.toggleOnBackground)
                    )

                    Spacer(Modifier.weight(1 - startSpacing))
                }

                Row {

                    ChatBottomIconToggleButton(
                        onClick = { onSetChatMode(ChatMode.Text) },
                        selected = chatMode == ChatMode.Text,
                        icon = R.drawable.keyboard,
                        contentDescription = "Text mode"
                    )

                    ChatBottomIconToggleButton(
                        onClick = { onSetChatMode(ChatMode.Speech) },
                        selected = chatMode == ChatMode.Speech,
                        icon = R.drawable.speech,
                        contentDescription = "Speech mode"
                    )
                }
            }

            Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
fun RowScope.ChatBottomIconToggleButton(
    onClick: () -> Unit,
    selected: Boolean,
    @DrawableRes icon: Int,
    contentDescription: String,
) {
    val theme = LocalAppTheme.current.chatBottomBar

    val iconColor by animateColorAsState(
        if (selected) {
            theme.toggleOnForeground
        } else {
            theme.toggleOffForeground
        }
    )

    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(100))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = iconColor
        )
    }
}

@Composable
fun UserChatBubble(text: String) {

    val theme = LocalAppTheme.current.chat

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Limit size to fraction
        Box(
            modifier = Modifier.fillMaxWidth(0.85f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(theme.bubbleBackgroundUser)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                theme.text.StyledText(text)
            }
        }
    }
}

@Composable
fun BotChatBubble(
    text: String,
    audioLevel: FloatState?
) {
    val theme = LocalAppTheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp),
    ) {
        val avatarSize by animateDpAsState(if (audioLevel != null) 72.dp else 36.dp)

        Box(
            Modifier
                .size(avatarSize)
                .border(1.dp, theme.chat.avatarBorderBot, CircleShape)
                .clip(CircleShape)
                .background(theme.activityBackground)
                .padding(8.dp)
        ) {
            if (audioLevel != null) {
                AudioIndicator(
                    modifier = Modifier.fillMaxSize(),
                    level = audioLevel,
                    color = theme.chat.foreground
                )
            }
        }

        Spacer(Modifier.width(15.dp))

        Markdown(
            content = text,
            colors = theme.chat.markdownColorsBot,
            typography = theme.chat.markdownTypography,
            modifier = Modifier
                .weight(1f)
                .padding(top = 6.dp)
        )
    }
}

@Composable
fun StatusBar(
    onDismiss: () -> Unit,
    text: String,
    background: Color,
    dismissContentDescription: String,
    loadingSpinner: Boolean = false,
) {
    val theme = LocalAppTheme.current.chat

    val animatedBackground by animateColorAsState(background)

    Row(
        Modifier
            .fillMaxWidth()
            .background(animatedBackground)
            .padding(vertical = 6.dp)
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (loadingSpinner) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                trackColor = Color.Transparent,
                color = theme.statusForeground
            )
        }

        theme.statusText.StyledText(
            modifier = Modifier.weight(1f),
            text = text
        )

        ToolbarIconButton(
            onClick = onDismiss,
            icon = R.drawable.close,
            color = theme.statusForeground,
            contentDescription = dismissContentDescription
        )
    }
}

@Composable
fun StatusBarListening(onDismiss: () -> Unit) {
    val theme = LocalAppTheme.current.chat
    StatusBar(
        text = "Listening",
        background = theme.statusBackgroundInCall,
        onDismiss = onDismiss,
        dismissContentDescription = "Disconnect"
    )
}

@Composable
fun StatusBarConnecting(onDismiss: () -> Unit) {
    val theme = LocalAppTheme.current.chat
    StatusBar(
        text = "Connecting...",
        background = theme.statusBackgroundNeutral,
        onDismiss = onDismiss,
        dismissContentDescription = "Cancel",
        loadingSpinner = true
    )
}

@Composable
@Preview
private fun PreviewStatusBarListening() {
    AppContextPreview {
        StatusBarListening({})
    }
}

@Composable
@Preview
private fun PreviewStatusBarConnecting() {
    AppContextPreview {
        StatusBarConnecting({})
    }
}

@Composable
@Preview
private fun PreviewChatBottomBar() {
    AppContextPreview {
        ChatBottomBar({}, {}, ChatMode.Text, userAudioLevel = null)
    }
}

@Composable
@Preview
private fun PreviewChatBottomBarSpeechQuiet() {
    AppContextPreview {
        ChatBottomBar({}, {}, ChatMode.Speech, userAudioLevel = null)
    }
}

@Composable
@Preview
private fun PreviewChatBottomBarSpeechLoud() {
    AppContextPreview {
        ChatBottomBar(
            {},
            {},
            ChatMode.Speech,
            userAudioLevel = remember { mutableFloatStateOf(1f) }
        )
    }
}

@Composable
@Preview
private fun PreviewUserChatBubbleSingleLine() {
    AppContextPreview {
        UserChatBubble("This is a test")
    }
}

@Composable
@Preview
private fun PreviewUserChatBubbleSingleLineWrapped() {
    AppContextPreview {
        UserChatBubble("This is a test with one very long line 1234 123 1231 2312 312")
    }
}

@Composable
@Preview
private fun PreviewUserChatBubbleMultiLine() {
    AppContextPreview {
        UserChatBubble("This is a test\nwith multiple lines")
    }
}

@Composable
@Preview
private fun PreviewBotChatBubbleSingleLine() {
    AppContextPreview {
        BotChatBubble("This is a test", audioLevel = null)
    }
}

@Composable
@Preview
private fun PreviewBotChatBubbleMultiLine() {
    AppContextPreview {
        BotChatBubble(
            "This is a test\nwith multiple lines",
            audioLevel = null
        )
    }
}

@Composable
@Preview
private fun PreviewBotSpeakingBubbleSilent() {
    AppContextPreview {
        BotChatBubble(
            "This is a test",
            remember { mutableFloatStateOf(0f) })
    }
}

@Composable
@Preview
private fun PreviewBotSpeakingBubbleLoud() {
    AppContextPreview {
        BotChatBubble(
            "This is a test",
            remember { mutableFloatStateOf(1f) })
    }
}