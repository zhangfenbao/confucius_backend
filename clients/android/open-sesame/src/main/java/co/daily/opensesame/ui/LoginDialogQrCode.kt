package co.daily.opensesame.ui

import ai.rtvi.client.result.Result
import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.daily.opensesame.Preferences
import co.daily.opensesame.api.DataApiRestApi
import co.daily.opensesame.utils.JSON_INSTANCE
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Composable
fun LoginDialogQrCode(
    onDismiss: () -> Unit,
    onLoggedIn: (url: String, token: String) -> Unit
) {
    var url by remember { mutableStateOf(Preferences.backendUrl.value ?: "") }
    var apiKey by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val currentUrl = url
    val currentKey = apiKey

    LaunchedEffect(currentUrl, currentKey) {
        if (!currentKey.isNullOrBlank()) {
            loading = true

            try {
                val api = DataApiRestApi(url, apiKey)

                api.getWorkspaces().withCallback {

                    loading = false

                    when (it) {
                        is Result.Err -> error = it.error.description
                        is Result.Ok -> onLoggedIn(url, currentKey)
                    }
                }

            } catch (e: Exception) {
                error = e.toString()
            }
        }
    }

    if (loading) {
        LoadingDialog()
    }

    error?.let {
        ErrorDialog(onDismissRequest = { error = null }, it)
    }

    Dialog(
        onDismissRequest = onDismiss,
        title = "Scan dashboard QR code",
        buttons = {
            DialogButton(
                onClick = onDismiss,
                text = "Cancel"
            )
            Space()
        }
    ) {
        Spacer(Modifier.height(12.dp))

        if (error == null) {
            QrCodeScanner(
                onQrCodeScanned = {
                    Log.i("LoginDialogQrCode", "Got QR code: $it")

                    try {
                        val obj = JSON_INSTANCE.decodeFromString<JsonObject>(it)

                        val newUrl = obj.getStr("baseUrl") ?: throw Exception()
                        val newKey = obj.getStr("token") ?: throw Exception()

                        Snapshot.withMutableSnapshot {
                            url = newUrl
                            apiKey = newKey
                        }

                    } catch(e: Exception) {
                        error = "Invalid QR code"
                    }
                },
                onError = { error = it}
            )
        }
    }
}

private fun JsonObject.getStr(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull