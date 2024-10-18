package co.daily.opensesame.utils

import ai.rtvi.client.result.Future
import ai.rtvi.client.result.withPromise
import ai.rtvi.client.utils.ThreadRef
import android.net.Uri
import co.daily.opensesame.api.DataApiError
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

private val httpClient = OkHttpClient.Builder()
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .connectTimeout(15, TimeUnit.SECONDS)
    .build()

fun httpRequest(
    thread: ThreadRef,
    url: String,
    method: HttpMethod,
    customHeaders: List<Pair<String, String>> = emptyList(),
    queryParams: List<Pair<String, String>> = emptyList(),
    responseHandler: ((InputStream) -> Unit)? = null
): Future<String, DataApiError> {

    return withPromise(thread) { promise ->
        thread {
            val result = try {
                val request = Request.Builder().apply {

                    val urlBuilder = url.toHttpUrl().newBuilder()

                    queryParams.forEach {
                        urlBuilder.addQueryParameter(it.first, it.second)
                    }

                    url(urlBuilder.build())

                    when (method) {
                        HttpMethod.Delete -> delete()
                        HttpMethod.Get -> get()
                        is HttpMethod.Post -> post(method.body)
                        is HttpMethod.Put -> put(method.body)
                    }

                    customHeaders.forEach {
                        header(it.first, it.second)
                    }
                }.build()

                httpClient.newCall(request).execute()

            } catch (e: Exception) {
                promise.resolveErr(DataApiError.ExceptionThrown(url, e))
                return@thread
            }

            if (result.isSuccessful && responseHandler != null) {

                val resultBody = result.body

                if (resultBody == null) {
                    promise.resolveErr(DataApiError.MissingResponseBody(url))
                    return@thread
                }

                try {
                    responseHandler(resultBody.byteStream())
                    promise.resolveOk("")
                } catch (e: Exception) {
                    promise.resolveErr(DataApiError.ExceptionThrown(url, e))
                }

                return@thread
            }

            val resultBody = try {
                result.body?.string()
            } catch (e: Exception) {
                promise.resolveErr(DataApiError.ExceptionThrown(url, e))
                return@thread
            }

            if (!result.isSuccessful) {
                promise.resolveErr(
                    DataApiError.BadStatusCode(
                        status = result.code,
                        body = resultBody,
                        url = url
                    )
                )
                return@thread
            }

            if (resultBody == null) {
                promise.resolveErr(DataApiError.MissingResponseBody(url))
                return@thread
            }

            promise.resolveOk(resultBody)
        }
    }
}

sealed interface HttpMethod {
    data object Get : HttpMethod
    data class Post(val body: RequestBody) : HttpMethod
    data class Put(val body: RequestBody) : HttpMethod
    data object Delete : HttpMethod
}

fun appendUrlPath(url: String, vararg pathElements: String): String? = try {
    Uri.parse(url).buildUpon().applyForEach(pathElements.asIterable()) { appendPath(it) }.build()
        .toString()
} catch (e: Exception) {
    null
}

private fun <E, R> R.applyForEach(values: Iterable<E>, action: R.(E) -> R): R {
    var result = this
    values.forEach {
        result = action(result, it)
    }
    return result
}