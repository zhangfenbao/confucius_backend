package co.daily.opensesame

import co.daily.opensesame.utils.Timestamp
import java.time.Duration
import java.util.UUID

object PreviewData {

    val workspaces = List(4) { i ->
        Workspace(
            id = WorkspaceId(UUID.randomUUID().toString()),
            title = "Workspace $i",
            createdAt = Timestamp.now() - Duration.ofDays(i.toLong() + 1),
            updatedAt = Timestamp.now() - Duration.ofHours(i.toLong() * 7),
        )
    }

    val conversations = workspaces.associate { workspace ->
        workspace.id to List(6) { i ->
            Conversation(
                conversationId = ConversationId(UUID.randomUUID().toString()),
                workspaceId = workspace.id,
                title = "Conversation $i",
                createdAt = Timestamp.now() - Duration.ofDays(i.toLong() + 1),
                updatedAt = Timestamp.now() - Duration.ofHours(i.toLong() * 7),
            )
        }
    }

    val conversationsById: Map<ConversationId, Conversation> =
        conversations.values.asSequence().flatten().associateBy { it.conversationId }

    val messages: Map<ConversationId, List<Message>> = conversationsById.mapValues { entry ->
        List(30) { i ->
            Message(
                id = MessageId.Permanent(UUID.randomUUID().toString()),
                stored = true,
                role = if (i % 2 == 0) MessageRole.User else MessageRole.Assistant,
                contentFinal = listOf(
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                    "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
                    "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.",
                    "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
                ).random(),
                contentPending = ""
            )
        }
    }
}