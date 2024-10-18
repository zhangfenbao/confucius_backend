package co.daily.opensesame.api

import androidx.compose.runtime.Immutable

@Immutable
sealed interface DataApiError {

    @Immutable
    data class BadStatusCode(val url: String, val status: Int, val body: String?) : DataApiError {
        override val description = "HTTP status $status for $url, body: '$body'"
    }

    @Immutable
    data class ExceptionThrown(val url: String, val e: Exception): DataApiError {
        override val description = "Exception thrown while fetching $url: $e"
    }

    @Immutable
    data class MissingResponseBody(val url: String): DataApiError {
        override val description = "Missing response body for $url"
    }

    @Immutable
    data class ConfigError(val message: String): DataApiError {
        override val description = "Config error: $message"
    }

    val description: String
}