package co.daily.opensesame.api

import ai.rtvi.client.result.Future
import ai.rtvi.client.result.Result
import ai.rtvi.client.result.resolvedPromiseErr
import ai.rtvi.client.result.withPromise
import ai.rtvi.client.types.Option
import ai.rtvi.client.utils.ThreadRef
import android.util.Log
import co.daily.opensesame.Conversation
import co.daily.opensesame.ConversationId
import co.daily.opensesame.Message
import co.daily.opensesame.RawMessage
import co.daily.opensesame.Workspace
import co.daily.opensesame.WorkspaceId
import co.daily.opensesame.utils.HttpMethod
import co.daily.opensesame.utils.httpRequest
import co.daily.opensesame.utils.mapDeserialize
import co.daily.opensesame.utils.toJsonBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.io.InputStream

private const val TAG = "DataApiRestApi"

class DataApiRestApi(
    private val baseUrl: String?,
    private val apiKey: String?,
) : DataApi {

    private val mainThread = ThreadRef.forMain()

    private fun makeRequest(
        path: String,
        method: HttpMethod,
        queryParams: List<Pair<String, String>> = emptyList(),
        responseHandler: ((InputStream) -> Unit)? = null
    ): Future<String, DataApiError> {

        if (baseUrl.isNullOrBlank()) {
            return resolvedPromiseErr(mainThread, DataApiError.ConfigError("API URL not set"))
        }

        if (apiKey.isNullOrBlank()) {
            return resolvedPromiseErr(mainThread, DataApiError.ConfigError("API key not set"))
        }

        return httpRequest(
            thread = mainThread,
            url = baseUrl + (if (baseUrl.endsWith("/")) "" else "/") + path,
            method = method,
            customHeaders = listOf("Authorization" to "Bearer $apiKey"),
            queryParams = queryParams,
            responseHandler = responseHandler
        ).logError()
    }

    override fun getWorkspaces(): Future<List<Workspace>, DataApiError> =
        makeRequest("api/workspaces", HttpMethod.Get).mapDeserialize<List<Workspace>>()
            .map404ToEmpty()

    override fun createWorkspace(
        title: String,
        config: JsonObject
    ): Future<Workspace, DataApiError> =
        makeRequest(
            "api/workspaces",
            HttpMethod.Post(
                RequestBodyCreateOrUpdateWorkspace(title = title, config = config)
                    .toJsonBody(RequestBodyCreateOrUpdateWorkspace.serializer())
            )
        ).mapDeserialize()

    override fun getWorkspace(id: WorkspaceId): Future<Workspace, DataApiError> =
        makeRequest("api/workspaces/${id.id}", HttpMethod.Get).logResult().mapDeserialize()

    override fun updateWorkspace(
        id: WorkspaceId,
        newTitle: String,
        newConfig: JsonObject
    ): Future<Workspace, DataApiError> =
        makeRequest(
            "api/workspaces/${id.id}",
            HttpMethod.Put(
                RequestBodyCreateOrUpdateWorkspace(title = newTitle, config = newConfig)
                    .toJsonBody(RequestBodyCreateOrUpdateWorkspace.serializer())
            )
        ).mapDeserialize()

    override fun deleteWorkspace(id: WorkspaceId): Future<Unit, DataApiError> =
        makeRequest("api/workspaces/${id.id}", HttpMethod.Delete).mapUnit()

    override fun getConversations(
        workspaceId: WorkspaceId,
        limit: Int,
        offset: Int
    ): Future<List<Conversation>, DataApiError> =
        makeRequest(
            "api/conversations/${workspaceId.id}",
            HttpMethod.Get,
            listOf("limit" to limit.toString(), "offset" to offset.toString())
        ).mapDeserialize<List<Conversation>>().map404ToEmpty()

    override fun createConversation(
        workspaceId: WorkspaceId,
        title: String,
        languageCode: String
    ): Future<Conversation, DataApiError> = makeRequest(
        "api/conversations",
        HttpMethod.Post(
            RequestBodyCreateConversation(
                workspaceId = workspaceId.id,
                title = title,
                languageCode = languageCode
            ).toJsonBody(RequestBodyCreateConversation.serializer())
        )
    ).mapDeserialize()

    override fun updateConversation(
        conversationId: ConversationId,
        newTitle: String,
    ): Future<Conversation, DataApiError> = makeRequest(
        "api/conversations/${conversationId.id}",
        HttpMethod.Put(
            RequestBodyUpdateConversation(
                title = newTitle,
            ).toJsonBody(RequestBodyUpdateConversation.serializer())
        )
    ).mapDeserialize()

    override fun deleteConversation(conversationId: ConversationId): Future<Unit, DataApiError> =
        makeRequest("api/conversations/${conversationId.id}", HttpMethod.Delete).mapUnit()

    override fun getConversationMessages(conversationId: ConversationId): Future<List<Message>, DataApiError> =
        makeRequest(
            "api/conversations/${conversationId.id}/messages",
            HttpMethod.Get
        ).mapDeserialize<ResponseBodyConversationMessages>()
            .map { it.messages.map { raw -> Message.fromRaw(raw) } }
            .map404ToEmpty()
}

private fun Future<String, DataApiError>.mapUnit() = map {}

@Serializable
private data class RequestBodyChatCompletion(
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("workspace_id")
    val workspaceId: String,
    val actions: List<RTVIMessage>
) {
    @Serializable
    data class RTVIMessage(
        val id: String,
        val label: String,
        val type: String,
        val data: Action,
    )

    @Serializable
    data class Action(
        val service: String,
        val action: String,
        val arguments: List<Option>
    )
}

@Serializable
private data class RequestBodyCreateOrUpdateWorkspace(
    val title: String,
    val config: JsonObject
)

@Serializable
private data class RequestBodyCreateConversation(
    @SerialName("workspace_id")
    val workspaceId: String,
    val title: String,
    @SerialName("language_code")
    val languageCode: String
)

@Serializable
private data class RequestBodyUpdateConversation(
    val title: String,
)

@Serializable
private data class ResponseBodyConversationMessages(
    val messages: List<RawMessage>
)

private fun <R> Future<R, DataApiError>.logError() = withErrorCallback {
    Log.e("DataApiRestApi", it.description)
}

private fun <R> Future<R, DataApiError>.logResult() = withCallback {
    Log.i("DataApiRestApi", it.toString())
}

private fun <R> Future<List<R>, DataApiError>.map404ToEmpty(): Future<List<R>, DataApiError> =
    withPromise(thread) { promise ->
        withCallback { result ->
            promise.resolve(
                when (result) {
                    is Result.Err -> if ((result.error as? DataApiError.BadStatusCode)?.status == 404) {
                        Result.Ok(emptyList())
                    } else {
                        result
                    }

                    is Result.Ok -> result
                }
            )
        }
    }