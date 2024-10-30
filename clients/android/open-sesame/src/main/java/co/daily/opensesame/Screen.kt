package co.daily.opensesame

sealed interface Screen {

    data object Workspaces : Screen {
        override val precedence = 10
    }

    data object AppConfig : Screen {
        override val precedence = 20
    }

    data object CreateWorkspace : Screen {
        override val precedence = 30
    }

    data class Chat(
        val workspaceId: WorkspaceId,
        val initialConversationId: ConversationId?
    ) : Screen {
        override val precedence = 40
    }

    data class EditWorkspace(
        val workspaceId: WorkspaceId,
        val cameFrom: Screen
    ) : Screen {
        override val precedence = 50
    }

    val precedence: Int
}