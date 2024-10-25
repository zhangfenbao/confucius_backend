package co.daily.opensesame.utils

import ai.rtvi.client.result.Future
import co.daily.opensesame.api.DataApiError
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

val JSON_INSTANCE = Json { ignoreUnknownKeys = true }

inline fun <reified E> E.toJsonBody(serializer: KSerializer<E>): RequestBody =
    JSON_INSTANCE.encodeToString(serializer, this).toRequestBody("application/json".toMediaType())

// TODO handle deserialization exception
inline fun <reified E> Future<String, DataApiError>.mapDeserialize(): Future<E, DataApiError> =
    map { JSON_INSTANCE.decodeFromString(it) }