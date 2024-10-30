package co.daily.opensesame.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import co.daily.opensesame.ui.theme.LocalAppTheme
import co.daily.opensesame.ui.theme.StyledText

class DialogButtonScope(private val rowScope: RowScope) {
    @Composable
    fun DialogButton(
        onClick: () -> Unit,
        text: String
    ) {
        val theme = LocalAppTheme.current.dialog

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onClick)
                .widthIn(min = 96.dp)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            theme.buttonText.StyledText(text)
        }
    }

    @Composable
    fun Space() {
        rowScope.apply {
            Spacer(Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    text: String? = null,
    buttons: @Composable (DialogButtonScope.() -> Unit)? = null,
    content: @Composable (ColumnScope.() -> Unit)? = null,
) {
    val theme = LocalAppTheme.current.dialog
    val scrollState = rememberScrollState()

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
    ) {
        val shape = RoundedCornerShape(12.dp)

        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .shadow(4.dp, shape)
                .border(1.dp, theme.border, shape)
                .clip(shape)
                .background(theme.background),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            @Composable
            fun HPad(content: @Composable ColumnScope.() -> Unit) {
                Column(Modifier.padding(horizontal = 22.dp), content = content)
            }

            Spacer(Modifier.height(12.dp))

            if (title != null) {
                HPad {
                    theme.title.StyledText(title)
                }
            }

            if (text != null) {
                HPad {
                    theme.message.StyledText(text)
                }
            }

            if (content != null) {
                HPad(content)
            }

            if (buttons != null) {

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.End)
                        .background(theme.buttonBackground)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    buttons(DialogButtonScope(this))
                }
            }
        }
    }
}

@Composable
fun DialogInputBox(
    onUpdated: (String) -> Unit,
    value: String,
    requestFocus: Boolean = false,
    hidePassword: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val theme = LocalAppTheme.current.dialog

    val focusRequester = remember { FocusRequester() }

    if (requestFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, theme.textInputBorder, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(theme.textInputBackground)
            .padding(horizontal = 15.dp, vertical = 8.dp)
    ) {
        BasicTextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            value = value,
            onValueChange = onUpdated,
            textStyle = theme.textInputText,
            cursorBrush = SolidColor(theme.textInputTextColor),
            visualTransformation = if (hidePassword) PasswordVisualTransformation() else VisualTransformation.None,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions
        )
    }
}

@Composable
fun ConfirmationDialog(
    onYes: () -> Unit,
    onNo: () -> Unit,
    title: String? = null,
    message: String? = null,
    yesText: String = "OK",
    noText: String = "Cancel"
) {
    Dialog(
        onDismissRequest = onNo,
        title = title,
        text = message,
        buttons = {
            DialogButton(onNo, noText)
            Space()
            DialogButton(onYes, yesText)
        }
    )
}