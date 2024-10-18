package co.daily.opensesame.api

import ai.rtvi.client.result.Future
import ai.rtvi.client.result.Result
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.daily.opensesame.ui.ConnectionError
import kotlinx.coroutines.delay

@Stable
class RepeatingFetchState<R>(default: R, cacheLookupResult: R?) {
    var initialFetchSucceeded by mutableStateOf(cacheLookupResult != null)
    var error by mutableStateOf<DataApiError?>(null)
    var retryCounter by mutableIntStateOf(0)
    var result by mutableStateOf<R>(cacheLookupResult ?: default)

    fun refresh() {
        retryCounter++
        error = null
    }
}

@Composable
fun <R> rememberRepeatingFetchState(key: Any?, default: R, cacheLookupResult: R?) = remember(key) {
    RepeatingFetchState(default, cacheLookupResult)
}

@Composable
fun <R> RepeatingFetch(state: RepeatingFetchState<R>, action: () -> Future<R, DataApiError>) {
    LaunchedEffect(state, state.retryCounter) {
        while (true) {
            when (val result = action().awaitNoThrow()) {
                is Result.Err -> {
                    state.error = result.error
                }

                is Result.Ok -> Snapshot.withMutableSnapshot {
                    state.result = result.value
                    state.initialFetchSucceeded = true
                }
            }

            // Fetch every 25 seconds
            delay(25000)
        }
    }
}

@Composable
fun <R> RepeatedFetchResultRenderer(
    modifier: Modifier,
    state: RepeatingFetchState<R>,
    loadingSpinnerColor: Color,
    content: @Composable (R) -> Unit
) {
    if (state.initialFetchSucceeded) {
        content(state.result)

    } else if (state.error != null) {
        ConnectionError(
            onRetryClicked = state::refresh,
            message = state.error?.description ?: "Unknown error",
            modifier = modifier
        )

    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = loadingSpinnerColor,
                strokeWidth = 4.dp
            )
        }
    }
}

fun <R> LazyListScope.RepeatedFetchResultRendererLazy(
    modifier: Modifier,
    state: RepeatingFetchState<R>,
    loadingSpinnerColor: Color,
    content: LazyListScope.(R) -> Unit
) {
    if (state.initialFetchSucceeded) {
        content(state.result)

    } else if (state.error != null) {
        item {
            ConnectionError(
                onRetryClicked = state::refresh,
                message = state.error?.description ?: "Unknown error",
                modifier = modifier
            )
        }

    } else {
        item {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = loadingSpinnerColor,
                    strokeWidth = 4.dp
                )
            }
        }
    }
}