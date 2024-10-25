package co.daily.opensesame.api

import ai.rtvi.client.result.Future
import ai.rtvi.client.result.Promise
import ai.rtvi.client.result.Result
import ai.rtvi.client.utils.ThreadRef
import co.daily.opensesame.Conversation
import co.daily.opensesame.ConversationId
import co.daily.opensesame.PreviewData
import co.daily.opensesame.Workspace
import co.daily.opensesame.WorkspaceId
import co.daily.opensesame.utils.MutexData
import co.daily.opensesame.utils.Timestamp
import kotlinx.serialization.json.JsonObject
import java.util.UUID

val mainThread = ThreadRef.forMain()

fun <S> afterDelay(delayMs: Long = 2000, action: () -> S): Future<S, DataApiError> {
    val promise = Promise<S, DataApiError>(mainThread)

    mainThread.handler.postDelayed({
        promise.resolveOk(action())
    }, delayMs)

    return promise
}

fun <S> resultAfterDelay(
    delayMs: Long = 2000,
    action: () -> Result<S, DataApiError>
): Future<S, DataApiError> {
    val promise = Promise<S, DataApiError>(mainThread)

    mainThread.handler.postDelayed({
        promise.resolve(action())
    }, delayMs)

    return promise
}

class DataApiPreview : DataApi {

    private val workspaces = MutexData(PreviewData.workspaces.associateBy { it.id }.toMutableMap())

    private val conversations =
        MutexData(PreviewData.conversations.mapValues { it.value.toMutableList() }.toMutableMap())

    private val conversationMessages =
        MutexData(PreviewData.messages.mapValues { it.value.toMutableList() }.toMutableMap())

    override fun getWorkspaces() = afterDelay { workspaces.lock { values.toList() } }

    //override fun getConversationMessages(conversationId: ConversationId) =
    //resultAfterDelay<List<Message>> { Result.Err(DataApiError.HTTPError(status = 400, url = "", body = null)) }

    override fun createWorkspace(
        title: String,
        config: JsonObject
    ) = afterDelay {
        val newWorkspace = Workspace(
            id = WorkspaceId(UUID.randomUUID().toString()),
            title = title,
            config = config,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        workspaces.lock {
            put(newWorkspace.id, newWorkspace)
        }

        conversations.lock {
            put(newWorkspace.id, mutableListOf())
        }

        newWorkspace
    }

    override fun getWorkspace(id: WorkspaceId) =
        afterDelay { workspaces.lock { get(id)!! } }

    override fun updateWorkspace(
        id: WorkspaceId,
        newTitle: String,
        newConfig: JsonObject
    ) = afterDelay {
        workspaces.lock {
            val newWorkspace = get(id)!!.copy(
                title = newTitle,
                config = newConfig,
                updatedAt = Timestamp.now()
            )

            put(newWorkspace.id, newWorkspace)

            newWorkspace
        }
    }

    override fun deleteWorkspace(id: WorkspaceId): Future<Unit, DataApiError> = afterDelay {
        workspaces.lock {
            remove(id)
        }
    }

    override fun getConversations(
        workspaceId: WorkspaceId,
        limit: Int,
        offset: Int
    ) = afterDelay {
        conversations.lock {
            get(workspaceId)!!.asSequence().drop(offset).take(limit).toList()
        }
    }

    override fun createConversation(
        workspaceId: WorkspaceId,
        title: String,
        languageCode: String
    ) = afterDelay {
        conversations.lock {
            val newConversation = Conversation(
                conversationId = ConversationId(UUID.randomUUID().toString()),
                workspaceId = workspaceId,
                title = title,
                archived = false,
                languageCode = languageCode,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            getOrPut(key = workspaceId, defaultValue = { mutableListOf() }).add(newConversation)

            newConversation
        }
    }

    override fun updateConversation(
        conversationId: ConversationId,
        newTitle: String
    ) = TODO()

    override fun deleteConversation(conversationId: ConversationId): Future<Unit, DataApiError> =
        afterDelay {
            conversations.lock {
                values.forEach { conversationsInWorkspace ->
                    conversationsInWorkspace.removeIf { conversation -> conversation.conversationId == conversationId }
                }
            }
        }

    override fun getConversationMessages(conversationId: ConversationId) =
        afterDelay {
            conversationMessages.lock {
                get(conversationId)?.toList() ?: emptyList()
            }
        }
}