package co.daily.opensesame.ui

import ai.rtvi.client.result.Result
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.daily.opensesame.LoadingOverlay
import co.daily.opensesame.Preferences
import co.daily.opensesame.R
import co.daily.opensesame.Theme
import co.daily.opensesame.ui.theme.LocalAppTheme
import co.daily.opensesame.ui.theme.StyledText

private enum class AuthDialogType {
    UsernamePassword,
    ApiKey,
    QrCode,
    LogOut
}

@Composable
fun AppConfigScreen(
    onClose: () -> Unit,
) {
    val theme = LocalAppTheme.current.appConfig
    val api = LocalDataApi.current

    var loading by remember { mutableStateOf(false) }
    val errors = remember { mutableStateListOf<String>() }
    var activeDialog by remember { mutableStateOf<AuthDialogType?>(null) }

    val dismissActiveDialog = { activeDialog = null }

    @Composable
    fun InputField(onValueUpdated: (String) -> Unit, value: String) {
        Box(
            Modifier
                //.border(1.dp, theme.textFieldBorder, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(theme.textFieldBackground)
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                onValueChange = onValueUpdated,
                cursorBrush = SolidColor(theme.foreground),
                textStyle = theme.textFieldText
            )
        }
    }

    val onLoggedIn: (url: String, token: String) -> Unit = { url, token ->
        Snapshot.withMutableSnapshot {
            Preferences.backendUrl.value = url
            Preferences.apiKey.value = token
        }
        dismissActiveDialog()
    }

    when (activeDialog) {

        AuthDialogType.UsernamePassword -> LoginDialogUsernamePassword(
            onDismiss = dismissActiveDialog,
            onLoggedIn = onLoggedIn
        )

        AuthDialogType.ApiKey -> LoginDialogApiKey(
            onDismiss = dismissActiveDialog,
            onLoggedIn = onLoggedIn
        )

        AuthDialogType.QrCode -> LoginDialogQrCode(
            onDismiss = dismissActiveDialog,
            onLoggedIn = onLoggedIn
        )

        AuthDialogType.LogOut -> ConfirmationDialog(
            onYes = {
                Preferences.apiKey.value = null
                dismissActiveDialog()
            },
            onNo = dismissActiveDialog,
            title = "Log out",
            message = "Are you sure you'd like to log out?",
            yesText = "Log out"
        )

        null -> {}
    }

    errors.firstOrNull()?.let { error ->
        ErrorDialog(onDismissRequest = { errors.removeFirstOrNull() }, text = error)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            ActionBar(title = "Configuration")

            val scrollState = rememberScrollState()

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(20.dp),
            ) {
                val menuTheme = LocalAppTheme.current.menuDarker

                theme.bodyText.FormattedText {
                    text("Open Sesame requires a hosted backend instance. For more details, see the ")
                    link("documentation", "https://github.com/daily-co/open-sesame")
                    text(".")
                }
                Spacer(Modifier.height(24.dp))

                theme.fieldHeader.StyledText("Authentication")
                Spacer(Modifier.height(4.dp))

                AnimatedContent(
                    modifier = Modifier.fillMaxWidth(),
                    targetState = !Preferences.apiKey.value.isNullOrBlank(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { isLoggedIn ->
                    Column {
                        if (isLoggedIn) {
                            theme.bodyText.base.StyledText("You are currently logged in.")
                            Spacer(Modifier.height(12.dp))

                            menuTheme.StyledMenu {
                                Group(title = null) {
                                    Item(
                                        name = "Log out",
                                        firstInList = true,
                                        onClick = { activeDialog = AuthDialogType.LogOut },
                                        icon = R.drawable.logout
                                    )
                                }
                            }
                        } else {
                            theme.bodyText.base.StyledText("Please select an authentication method.")
                            Spacer(Modifier.height(12.dp))

                            menuTheme.StyledMenu {
                                Group(title = null) {
                                    Item(
                                        name = "Username and password",
                                        firstInList = true,
                                        onClick = {
                                            activeDialog = AuthDialogType.UsernamePassword
                                        },
                                        icon = R.drawable.form_textbox_password
                                    )

                                    Item(
                                        name = "API key",
                                        onClick = { activeDialog = AuthDialogType.ApiKey },
                                        icon = R.drawable.key
                                    )

                                    Item(
                                        name = "Scan QR code",
                                        onClick = { activeDialog = AuthDialogType.QrCode },
                                        icon = R.drawable.qrcode_scan
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                theme.fieldHeader.StyledText("Base URL")
                Spacer(Modifier.height(4.dp))

                theme.bodyText.base.StyledText("The URL of your Open Sesame backend instance.")
                Spacer(Modifier.height(2.dp))

                theme.bodyTextHint.StyledText("For example: https://yourapp.modal.run")
                Spacer(Modifier.height(12.dp))

                InputField(
                    onValueUpdated = { Preferences.backendUrl.value = it },
                    value = Preferences.backendUrl.value ?: ""
                )

                Spacer(Modifier.height(24.dp))

                theme.fieldHeader.StyledText("Theme")
                Spacer(Modifier.height(12.dp))

                menuTheme.StyledMenu {
                    Group(title = null) {

                        val currentVal =
                            Preferences.theme.value?.let(Theme::fromStr) ?: Theme.System

                        @Composable
                        fun ThemeItem(
                            name: String,
                            value: Theme,
                            @DrawableRes icon: Int,
                            firstInList: Boolean = false
                        ) {
                            Item(
                                name = name,
                                firstInList = firstInList,
                                onClick = { Preferences.theme.value = value.strVal },
                                secondary = MenuItemSecondary.Radio(currentVal == value),
                                icon = icon
                            )
                        }

                        ThemeItem(
                            "Light",
                            Theme.Light,
                            R.drawable.weather_sunny,
                            firstInList = true
                        )
                        ThemeItem("Dark", Theme.Dark, R.drawable.moon_waning_crescent)
                        ThemeItem("Auto", Theme.System, R.drawable.cellphone_cog)
                    }
                }

                Spacer(Modifier.height(36.dp))


            }

            Column(Modifier.fillMaxWidth()) {

                HorizontalDivider(
                    thickness = 1.dp,
                    color = theme.textFieldBorder
                )

                Box(Modifier.padding(20.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = {
                                loading = true
                                api
                                    .getWorkspaces()
                                    .withCallback {
                                        loading = false

                                        when (it) {
                                            is Result.Err -> errors.add("Failed to verify details: ${it.error.description}")
                                            is Result.Ok -> onClose()
                                        }
                                    }
                            })
                            .background(theme.buttonBackground)
                            .padding(horizontal = 20.dp, vertical = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        theme.buttonText.StyledText("Save")
                    }
                }
            }
        }

        if (loading) {
            LoadingOverlay()
        }
    }

}

@Preview
@Composable
private fun PreviewAppConfigScreen() {
    AppContextPreview {
        AppConfigScreen({})
    }
}