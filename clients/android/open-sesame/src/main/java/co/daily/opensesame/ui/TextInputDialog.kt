package co.daily.opensesame.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun TextInputDialogLive(
    onUpdated: (String) -> Unit,
    onDismissRequest: () -> Unit,
    title: String,
    value: String
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = title,
        buttons = {
            DialogButton(
                onClick = onDismissRequest,
                text = "OK"
            )
        }
    ) {
        DialogInputBox(onUpdated = onUpdated, requestFocus = true, value = value)
    }
}

@Composable
fun TextInputDialogSave(
    onSave: (String) -> Unit,
    onDismissRequest: () -> Unit,
    title: String,
    initialValue: String = ""
) {
    var value by remember { mutableStateOf(initialValue) }

    Dialog(
        onDismissRequest = onDismissRequest,
        title = title,
        buttons = {
            DialogButton(
                onClick = onDismissRequest,
                text = "Cancel"
            )
            Space()
            DialogButton(
                onClick = { onSave(value) },
                text = "Save"
            )
        }
    ) {
        DialogInputBox(onUpdated = { value = it }, value = value, requestFocus = true)
    }
}

@Preview
@Composable
private fun PreviewTextDialogLive() {
    AppContextPreview {
        TextInputDialogLive(
            onUpdated = {},
            onDismissRequest = {},
            title = "Title",
            value = ""
        )
    }
}

@Preview
@Composable
private fun PreviewTextDialogSave() {
    AppContextPreview {
        TextInputDialogSave(
            onSave = {},
            onDismissRequest = {},
            title = "Title",
        )
    }
}