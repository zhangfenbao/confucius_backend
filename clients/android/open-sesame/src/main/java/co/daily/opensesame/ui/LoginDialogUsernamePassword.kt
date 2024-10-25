package co.daily.opensesame.ui

import ai.rtvi.client.result.Result
import ai.rtvi.client.utils.ThreadRef
import android.os.Build
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.daily.opensesame.Preferences
import co.daily.opensesame.ui.theme.LocalAppTheme
import co.daily.opensesame.ui.theme.StyledText
import co.daily.opensesame.utils.HttpMethod
import co.daily.opensesame.utils.appendUrlPath
import co.daily.opensesame.utils.httpRequest
import co.daily.opensesame.utils.mapDeserialize
import co.daily.opensesame.utils.toJsonBody
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

fun deviceModel() = if (Build.MODEL.startsWith("Android SDK")) {
    "Android emulator"
} else {
    Build.MODEL
}

@Composable
fun LoginDialogUsernamePassword(
    onDismiss: () -> Unit,
    onLoggedIn: (url: String, token: String) -> Unit
) {
    val theme = LocalAppTheme.current.dialog

    var url by remember { mutableStateOf(Preferences.backendUrl.value ?: "") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val errors = remember { mutableStateListOf<String>() }
    var loading by remember { mutableStateOf(false) }

    val attemptLogin: () -> Unit = {
        loading = true

        val sessionUrl = appendUrlPath(url, "api", "users", "token")

        if (sessionUrl == null) {
            errors.add("Invalid URL")
        } else {
            fun jsonObj(vararg elements: Pair<String, JsonElement>) = JsonObject(elements.toMap())

            httpRequest(
                thread = ThreadRef.forMain(),
                url = sessionUrl,
                method = HttpMethod.Post(
                    jsonObj(
                        "title" to JsonPrimitive(deviceModel()),
                        "user" to jsonObj(
                            "username" to JsonPrimitive(username),
                            "password" to JsonPrimitive(password)
                        )
                    ).toJsonBody(JsonObject.serializer())
                ),
            )
                .mapDeserialize<JsonObject>()
                .withCallback {

                    loading = false

                    when (it) {
                        is Result.Err -> {
                            errors.add(it.error.description)
                        }

                        is Result.Ok -> {
                            val token = (it.value.get("token") as? JsonPrimitive)?.contentOrNull

                            if (token == null) {
                                errors.add("Token field in response was null")
                            } else {
                                onLoggedIn(url, token)
                            }
                        }
                    }
                }
        }
    }

    if (loading) {
        LoadingDialog()
    }

    errors.firstOrNull()?.let { error ->
        ErrorDialog(onDismissRequest = errors::removeFirstOrNull, error)
    }

    Dialog(
        onDismissRequest = onDismiss,
        title = "Log in",
        buttons = {
            DialogButton(
                onClick = onDismiss,
                text = "Cancel"
            )
            Space()
            DialogButton(
                onClick = attemptLogin,
                text = "Log in"
            )
        }
    ) {


        Spacer(Modifier.height(12.dp))

        theme.message.StyledText("Backend URL")

        DialogInputBox(
            onUpdated = { url = it },
            requestFocus = true,
            value = url,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next
            )
        )

        Spacer(Modifier.height(12.dp))

        theme.message.StyledText("Username")

        DialogInputBox(
            onUpdated = { username = it },
            value = username,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )

        Spacer(Modifier.height(12.dp))

        theme.message.StyledText("Password")

        DialogInputBox(
            onUpdated = { password = it },
            value = password,
            hidePassword = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(onGo = { attemptLogin() })
        )
    }
}

@Composable
@Preview
private fun PreviewLoginDialog() {
    LoginDialogUsernamePassword({}, { _, _ -> })
}