package co.daily.opensesame.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ErrorDialog(
    onDismissRequest: () -> Unit,
    text: String
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = "Error",
        text = text,
        buttons = {
            DialogButton(
                onClick = onDismissRequest,
                text = "Close"
            )
        }
    )
}

@Composable
@Preview
private fun PreviewErrorDialog() {
    AppContextPreview {
        ErrorDialog(onDismissRequest = {}, text = "Preview error message")
    }
}