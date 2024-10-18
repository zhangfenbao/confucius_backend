package co.daily.opensesame.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.daily.opensesame.R
import co.daily.opensesame.ui.theme.LocalAppTheme
import co.daily.opensesame.ui.theme.MenuTheme
import co.daily.opensesame.ui.theme.StyledText


@DslMarker
annotation class MenuScopeDslMarker

@MenuScopeDslMarker
class MenuScope(private val theme: MenuTheme) {
    @Composable
    fun Group(
        title: String?,
        groupStyle: Modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(theme.itemBackground),
        content: @Composable MenuGroupScope.() -> Unit
    ) {

        Column {
            if (title != null) {
                theme.headerText.StyledText(
                    title.uppercase(),
                    Modifier.padding(horizontal = 15.dp)
                )

                Spacer(Modifier.height(9.dp))
            }

            Column(groupStyle) {
                content(MenuGroupScope(theme))
            }
        }
    }

    @Composable
    fun Button(
        onClick: () -> Unit,
        name: String
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .background(theme.buttonBackground)
                .padding(horizontal = 20.dp, vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            theme.buttonText.StyledText(name)
        }
    }
}

@DslMarker
annotation class MenuGroupScopeDslMarker

@MenuGroupScopeDslMarker
class MenuGroupScope(private val theme: MenuTheme) {

    @Composable
    fun Item(
        onClick: (() -> Unit)?,
        name: String,
        @DrawableRes icon: Int? = null,
        firstInList: Boolean = false,
        secondary: MenuItemSecondary? = null,
        subtitle: String? = null
    ) {
        if (!firstInList) {
            HorizontalDivider(
                color = theme.divider,
                thickness = theme.dividerThickness
            )
        }

        Row(
            Modifier
                .heightIn(min = 46.dp)
                .applyIfNotNull(onClick) { clickable(onClick = it) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {

                Spacer(Modifier.width(14.dp))

                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = theme.itemForeground
                )

                Spacer(Modifier.width(10.dp))

            } else {
                Spacer(Modifier.width(16.dp))
            }

            val hasRightAlignedText =
                secondary is MenuItemSecondary.Chevron && secondary.text != null

            Column(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .applyIf(!hasRightAlignedText) { weight(1f) },
            ) {
                theme.itemText.StyledText(name)

                if (subtitle != null) {
                    theme.subtitle.StyledText(subtitle)
                }
            }

            Spacer(Modifier.width(16.dp))

            when (secondary) {

                is MenuItemSecondary.Checkbox -> {
                    Switch(checked = secondary.checked, onCheckedChange = null)
                    Spacer(Modifier.width(12.dp))
                }

                is MenuItemSecondary.Radio -> {
                    RadioButton(selected = secondary.selected, onClick = null)
                    Spacer(Modifier.width(12.dp))
                }

                is MenuItemSecondary.Chevron -> {
                    if (secondary.text != null) {
                        theme.itemSecondaryText.StyledText(
                            modifier = Modifier.weight(1f),
                            text = secondary.text,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            textAlign = TextAlign.Right
                        )
                        Spacer(Modifier.width(2.dp))
                    }

                    Icon(
                        modifier = Modifier.size(28.dp),
                        painter = painterResource(R.drawable.chevron_right),
                        contentDescription = null,
                        tint = theme.itemForegroundChevron
                    )

                    Spacer(Modifier.width(6.dp))
                }

                null -> {
                    Spacer(Modifier.width(10.dp))
                }
            }
        }
    }
}

@Immutable
sealed interface MenuItemSecondary {

    @Immutable
    data class Chevron(val text: String? = null) : MenuItemSecondary

    @Immutable
    data class Checkbox(val checked: Boolean) : MenuItemSecondary

    @Immutable
    data class Radio(val selected: Boolean) : MenuItemSecondary

}

@Composable
private fun Menu(
    modifier: Modifier = Modifier,
    theme: MenuTheme,
    content: @Composable MenuScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        content(MenuScope(theme))
    }
}

@Composable
fun MenuTheme.StyledMenu(
    modifier: Modifier = Modifier,
    content: @Composable MenuScope.() -> Unit
) {
    Menu(modifier = modifier, theme = this, content = content)
}

@Composable
@Preview(backgroundColor = 0xFF000000, showBackground = true)
private fun MenuPreview() {
    AppContextPreview {
        LocalAppTheme.current.menuDarker.StyledMenu {
            Group("Configuration") {
                Item(
                    name = "Model",
                    onClick = null,
                    firstInList = true,
                    secondary = MenuItemSecondary.Chevron("GPT-4o")
                )
                Item(
                    name = "Prompt",
                    onClick = null,
                    icon = R.drawable.video,
                    secondary = MenuItemSecondary.Chevron()
                )
                Item(
                    name = "Data storage",
                    onClick = null,
                    secondary = MenuItemSecondary.Checkbox(false)
                )
                Item(
                    name = "Radio",
                    onClick = null,
                    secondary = MenuItemSecondary.Radio(true),
                    subtitle = "Test subtitle"
                )
            }

            Button({}, "Save")
        }
    }
}