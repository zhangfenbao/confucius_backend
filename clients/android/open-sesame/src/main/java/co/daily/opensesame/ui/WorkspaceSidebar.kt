package co.daily.opensesame.ui

import ai.rtvi.client.result.Result
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.daily.opensesame.Conversation
import co.daily.opensesame.ConversationId
import co.daily.opensesame.PreviewData
import co.daily.opensesame.R
import co.daily.opensesame.WorkspaceId
import co.daily.opensesame.api.RepeatedFetchResultRendererLazy
import co.daily.opensesame.api.RepeatingFetch
import co.daily.opensesame.api.rememberRepeatingFetchState
import co.daily.opensesame.ui.theme.LocalAppTheme
import co.daily.opensesame.ui.theme.StyledText
import co.daily.opensesame.utils.rtcStateMins
import kotlinx.coroutines.launch

@Composable
fun WorkspaceDrawer(
    onConversationSelected: (ConversationId) -> Unit,
    onWorkspacesSelected: () -> Unit,
    drawerState: DrawerState,
    selectedWorkspace: WorkspaceId,
    selectedChat: ConversationId?,
    mainContent: @Composable () -> Unit
) {
    val theme = LocalAppTheme.current.workspaceSidebar

    val scope = rememberCoroutineScope()

    val api = LocalDataApi.current
    val fetchState = rememberRepeatingFetchState<List<Conversation>>(
        key = selectedWorkspace,
        default = emptyList(),
        cacheLookupResult = null
    )

    var loading by remember { mutableIntStateOf(0) }
    val errors = remember { mutableStateListOf<String>() }

    if (loading > 0) {
        LoadingDialog()
    }

    errors.firstOrNull()?.let {
        ErrorDialog(onDismissRequest = { errors.removeFirstOrNull() }, text = it)
    }

    // TODO pagination
    RepeatingFetch(fetchState) { api.getConversations(selectedWorkspace, limit = 50, offset = 0) }

    val selectedWorkspaceName =
        api.cacheLookupWorkspaceName(selectedWorkspace) ?: "Unnamed workspace"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = theme.background
            ) {
                if (drawerState.isOpen) {
                    BackHandler {
                        scope.launch {
                            drawerState.close()
                        }
                    }
                }

                val now by rtcStateMins()

                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 15.dp)
                ) {
                    item {
                        Column {

                            Spacer(Modifier.height(48.dp))

                            WorkspaceDrawerHeader(
                                title = selectedWorkspaceName,
                                iconColor = Color(0xFFA78BFA)
                            )

                            Spacer(Modifier.height(32.dp))
                        }
                    }

                    item {
                        Column {
                            WorkspaceAction(
                                onClick = {
                                    loading++
                                    api.createConversation(
                                        workspaceId = selectedWorkspace,
                                        title = "Untitled",
                                        languageCode = "english" // TODO
                                    ).withCallback {
                                        loading--
                                        fetchState.refresh()
                                        when (it) {
                                            is Result.Err -> errors.add(it.error.description)
                                            is Result.Ok -> {
                                                onConversationSelected(it.value.conversationId)
                                            }
                                        }
                                    }
                                },
                                icon = R.drawable.edit,
                                name = "New chat"
                            )
                            WorkspaceAction(
                                onClick = onWorkspacesSelected,
                                icon = R.drawable.workspaces,
                                name = "Workspaces"
                            )
                        }
                    }

                    RepeatedFetchResultRendererLazy(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        state = fetchState,
                        loadingSpinnerColor = theme.itemForeground,
                    ) { conversations ->
                        WorkspaceChatList(
                            chats = ImmutableMarker(conversations),
                            onChatSelected = onConversationSelected,
                            selectedChat = selectedChat,
                            now = now,
                            theme = theme
                        )
                    }
                }
            }
        },
        content = mainContent
    )
}

@Composable
fun WorkspaceAction(
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    name: String,
) {
    val theme = LocalAppTheme.current.workspaceSidebar

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(icon),
            tint = theme.itemForeground,
            contentDescription = null
        )

        theme.actionText.StyledText(
            modifier = Modifier.weight(1f),
            text = name
        )
    }
}

@Composable
fun WorkspaceDrawerHeader(
    title: String,
    iconColor: Color,
) {
    val theme = LocalAppTheme.current.workspaceSidebar

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        WorkspaceIcon(color = iconColor)

        theme.titleText.StyledText(
            modifier = Modifier.weight(1f),
            text = title
        )
    }
}

@Composable
fun WorkspaceIcon(
    color: Color
) {
    val theme = LocalAppTheme.current.workspaceSidebar

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color)
            .padding(12.dp)
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(R.drawable.frame),
            contentDescription = null,
            tint = theme.iconForeground
        )
    }
}

@Composable
@Preview
private fun PreviewWorkspaceDrawer() {
    AppContextPreview {
        WorkspaceDrawer(
            onConversationSelected = {},
            onWorkspacesSelected = {},
            selectedChat = PreviewData.conversations.values.first().first().conversationId,
            selectedWorkspace = PreviewData.workspaces.first().id,
            drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
        ) {}
    }
}