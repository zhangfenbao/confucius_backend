package co.daily.opensesame.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.daily.opensesame.R
import co.daily.opensesame.ui.theme.LocalAppTheme
import co.daily.opensesame.ui.theme.StyledText

@Composable
fun ConnectionError(onRetryClicked: () -> Unit, message: String, modifier: Modifier = Modifier) {

    val theme = LocalAppTheme.current.connectionError

    Column(
        modifier = modifier.padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            painter = painterResource(R.drawable.alert_circle_outline),
            tint = theme.errorIcon,
            contentDescription = "Error"
        )
        theme.errorTitle.StyledText("Connection error")

        theme.errorMessage.StyledText(text = message, textAlign = TextAlign.Center)

        Spacer(Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100))
                .clickable(onClick = onRetryClicked)
                .background(theme.errorButton)
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            theme.errorButtonText.StyledText("Try again")
        }
    }
}

@Composable
@Preview
private fun PreviewConnectionError() {
    AppContextPreview {
        ConnectionError(
            onRetryClicked = {},
            message = "Error message",
            modifier = Modifier.fillMaxWidth()
        )
    }
}