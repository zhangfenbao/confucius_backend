package co.daily.opensesame.api

import ai.rtvi.client.result.Future
import co.daily.opensesame.Conversation
import co.daily.opensesame.ConversationId
import co.daily.opensesame.Message
import co.daily.opensesame.Workspace
import co.daily.opensesame.WorkspaceId
import kotlinx.serialization.json.JsonObject

interface DataApi {

    fun getWorkspaces(): Future<List<Workspace>, DataApiError>

    fun createWorkspace(
        title: String,
        config: JsonObject
    ): Future<Workspace, DataApiError>

    fun getWorkspace(id: WorkspaceId): Future<Workspace, DataApiError>

    fun updateWorkspace(
        id: WorkspaceId,
        newTitle: String,
        newConfig: JsonObject
    ): Future<Workspace, DataApiError>

    fun deleteWorkspace(id: WorkspaceId): Future<Unit, DataApiError>

    fun getConversations(
        workspaceId: WorkspaceId,
        limit: Int,
        offset: Int
    ): Future<List<Conversation>, DataApiError>

    fun createConversation(
        workspaceId: WorkspaceId,
        title: String,
        languageCode: String
    ): Future<Conversation, DataApiError>

    fun updateConversation(
        conversationId: ConversationId,
        newTitle: String,
    ): Future<Conversation, DataApiError>

    fun deleteConversation(conversationId: ConversationId): Future<Unit, DataApiError>

    fun getConversationMessages(conversationId: ConversationId): Future<List<Message>, DataApiError>
}

