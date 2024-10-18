package co.daily.opensesame.api

import ai.rtvi.client.result.Future
import ai.rtvi.client.result.Result
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import co.daily.opensesame.ConfigConstants
import co.daily.opensesame.Conversation
import co.daily.opensesame.ConversationId
import co.daily.opensesame.Workspace
import co.daily.opensesame.WorkspaceConfigRaw
import co.daily.opensesame.WorkspaceId
import co.daily.opensesame.utils.JSON_INSTANCE
import co.daily.opensesame.utils.MutexData
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class CachingDataApi(private val base: DataApi) : DataApi {

    private val cache = MutexData(CacheState())

    private val workspaceNames = mutableStateMapOf<WorkspaceId, String?>()
    private val workspaceModes = mutableStateMapOf<WorkspaceId, ConfigConstants.InteractionMode?>()
    private val chatNames = mutableStateMapOf<ConversationId, String?>()

    private fun updateCacheWorkspace(workspace: Workspace) {
        cache.lock {
            workspaces[workspace.id] = workspace
            workspaceNames[workspace.id] = workspace.title

            try {
                val interactionMode = workspace.config
                    ?.let<JsonElement, WorkspaceConfigRaw>(JSON_INSTANCE::decodeFromJsonElement)
                    ?.interactionMode
                    ?.let(ConfigConstants.InteractionMode::fromId)

                workspaceModes[workspace.id] = interactionMode
            } catch (e: Exception) {
                Log.e("CachingDataApi", "Failed to parse config", e)
            }
        }
    }

    private fun updateCacheConversation(conversation: Conversation) {
        cache.lock {
            chatNames[conversation.conversationId] = conversation.title
        }
    }

    fun cacheLookupWorkspace(workspaceId: WorkspaceId): Workspace? =
        cache.lock { workspaces[workspaceId] }

    fun cacheLookupAllWorkspaces(): List<Workspace>? =
        cache.lock { workspaces.values.takeUnless { it.isEmpty() }?.toList() }

    fun cacheLookupWorkspaceName(workspaceId: WorkspaceId): String? = workspaceNames[workspaceId]

    fun cacheLookupWorkspaceMode(workspaceId: WorkspaceId): ConfigConstants.InteractionMode? =
        workspaceModes[workspaceId]

    fun cacheLookupChatName(conversationId: ConversationId): String? = chatNames[conversationId]

    override fun getWorkspaces() = base.getWorkspaces().withCallbackOnOk {
        cache.lock {
            workspaces.clear()
            it.forEach { workspace ->
                workspaces[workspace.id] = workspace
                workspaceNames[workspace.id] = workspace.title
            }
        }
    }

    override fun createWorkspace(
        title: String,
        config: JsonObject
    ) = base.createWorkspace(title = title, config = config)
        .withCallbackOnOk(::updateCacheWorkspace)

    override fun getWorkspace(id: WorkspaceId) =
        base.getWorkspace(id).withCallbackOnOk(::updateCacheWorkspace)

    override fun updateWorkspace(
        id: WorkspaceId,
        newTitle: String,
        newConfig: JsonObject
    ) = base.updateWorkspace(id, newTitle, newConfig).withCallbackOnOk(::updateCacheWorkspace)

    override fun deleteWorkspace(id: WorkspaceId) = base.deleteWorkspace(id).withCallbackOnOk {
        cache.lock {
            workspaces.remove(id)
            workspaceNames.remove(id)
        }
    }

    override fun getConversations(
        workspaceId: WorkspaceId,
        limit: Int,
        offset: Int
    ) = base.getConversations(workspaceId = workspaceId, limit = limit, offset = offset)
        .withCallbackOnOk {
            cache.lock {
                it.forEach(::updateCacheConversation)
            }
        }

    override fun createConversation(
        workspaceId: WorkspaceId,
        title: String,
        languageCode: String
    ) = base.createConversation(
        workspaceId = workspaceId,
        title = title,
        languageCode = languageCode
    ).withCallbackOnOk {
        cache.lock {
            updateCacheConversation(it)
        }
    }

    override fun updateConversation(
        conversationId: ConversationId,
        newTitle: String,
    ): Future<Conversation, DataApiError> =
        base.updateConversation(conversationId, newTitle).withCallbackOnOk {
            cache.lock {
                updateCacheConversation(it)
            }
        }

    override fun deleteConversation(conversationId: ConversationId) =
        base.deleteConversation(conversationId).withCallbackOnOk {
            cache.lock {
                chatNames.remove(conversationId)
            }
        }

    override fun getConversationMessages(conversationId: ConversationId) =
        base.getConversationMessages(conversationId)
}

class CacheState {
    val workspaces: MutableMap<WorkspaceId, Workspace> = mutableMapOf()
}

private fun <V, E> Future<V, E>.withCallbackOnOk(callback: (V) -> Unit) = withCallback {
    if (it is Result.Ok) {
        callback(it.value)
    }
}