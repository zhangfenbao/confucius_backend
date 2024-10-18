package co.daily.opensesame.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.daily.opensesame.R
import co.daily.opensesame.ui.theme.LocalAppTheme
import co.daily.opensesame.ui.theme.StyledText

@Composable
fun ActionBar(
    onNavButtonClicked: (() -> Unit)? = null,
    onSettingsButtonClicked: (() -> Unit)? = null,
    @DrawableRes settingsButtonIcon: Int = R.drawable.settings,
    onEditButtonClicked: (() -> Unit)? = null,
    title: String,
) {
    val theme = LocalAppTheme.current.actionBar

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onNavButtonClicked != null) {
            ToolbarIconButton(
                onClick = onNavButtonClicked,
                icon = R.drawable.menu,
                contentDescription = "Navigation menu",
                color = theme.foreground
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            theme.title.StyledText(
                modifier = Modifier.weight(1f),
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            if (onEditButtonClicked != null) {
                ToolbarIconButton(
                    onClick = onEditButtonClicked,
                    icon = R.drawable.edit,
                    color = theme.secondaryForeground,
                    contentDescription = "Edit title"
                )
            }
        }

        if (onSettingsButtonClicked != null) {
            ToolbarIconButton(
                onClick = onSettingsButtonClicked,
                icon = settingsButtonIcon,
                contentDescription = "Settings",
                color = theme.secondaryForeground
            )
        }
    }
}

@Composable
@Preview
private fun PreviewActionBar() {
    AppContextPreview {
        ActionBar(
            onNavButtonClicked = {},
            onSettingsButtonClicked = {},
            onEditButtonClicked = {},
            title = "Default Workspace 123 456 789 10 11 12"
        )
    }
}