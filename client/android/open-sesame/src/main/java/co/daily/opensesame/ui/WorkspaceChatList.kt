package co.daily.opensesame.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.daily.opensesame.Conversation
import co.daily.opensesame.ConversationId
import co.daily.opensesame.PreviewData
import co.daily.opensesame.ui.theme.LocalAppTheme
import co.daily.opensesame.ui.theme.StyledText
import co.daily.opensesame.ui.theme.WorkspaceSidebarTheme
import co.daily.opensesame.utils.Timestamp
import co.daily.opensesame.utils.rtcStateMins

fun LazyListScope.WorkspaceChatList(
    chats: ImmutableMarker<List<Conversation>>,
    onChatSelected: (ConversationId) -> Unit,
    selectedChat: ConversationId?,
    now: Timestamp,
    theme: WorkspaceSidebarTheme
) {
    // Chats arrive from the backend in descending order by updated time
    val groupedByDate = chats.value.groupBy { it.updatedAt.asCalendarDate().toMenuGroup(now) }

    for (entry in groupedByDate) {

        item {
            Spacer(Modifier.height(32.dp))

            HorizontalDivider(
                modifier = Modifier.animateItem(),
                thickness = 1.dp,
                color = theme.divider
            )

            Spacer(Modifier.height(32.dp))

            theme.headerText.StyledText(
                modifier = Modifier.animateItem().padding(horizontal = 15.dp),
                text = entry.key.uppercase()
            )

            Spacer(Modifier.height(9.dp))
        }

        items(
            items = entry.value,
            key = { it.conversationId.id }
        ) {
            val api = LocalDataApi.current

            val isSelected = it.conversationId == selectedChat

            val background by animateColorAsState(
                if (isSelected) {
                    theme.chatItemBackgroundSelected
                } else {
                    Color.Transparent
                }
            )

            val title = api.cacheLookupChatName(it.conversationId) ?: it.title ?: "Untitled"

            Box(
                modifier = Modifier
                    .animateItem()
                    .clip(RoundedCornerShape(12.dp))
                    .fillMaxWidth()
                    .clickable { onChatSelected(it.conversationId) }
                    .background(background)
                    .padding(horizontal = 15.dp, vertical = 12.dp)
            ) {
                theme.chatItemText.StyledText(text = title)
            }
        }
    }
}

@Composable
@Preview
private fun PreviewWorkspaceChatList() {
    AppContextPreview {

        val now by rtcStateMins()
        val theme = LocalAppTheme.current.workspaceSidebar

        LazyColumn {
            WorkspaceChatList(
                chats = ImmutableMarker(PreviewData.conversations.values.first()),
                onChatSelected = {},
                selectedChat = ConversationId("id 2"),
                now = now,
                theme = theme
            )
        }
    }
}