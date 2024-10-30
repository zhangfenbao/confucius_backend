package co.daily.opensesame.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import co.daily.opensesame.ui.FormattedTextStyle
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.MarkdownColors

@Composable
fun RTVIClientTheme(
    lightMode: Boolean,
    content: @Composable () -> Unit
) {
    val theme = if (lightMode) AppThemeLight else AppThemeDark

    val activity = LocalContext.current as? Activity

    val insetsController =
        activity?.let { WindowCompat.getInsetsController(it.window, LocalView.current) }

    LaunchedEffect(lightMode) {
        insetsController?.isAppearanceLightStatusBars = lightMode
        insetsController?.isAppearanceLightNavigationBars = lightMode

        activity?.window?.navigationBarColor = if (lightMode) {
            android.graphics.Color.WHITE
        } else {
            android.graphics.Color.BLACK
        }
    }

    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(
            colorScheme = theme.material,
            content = content
        )
    }
}

@Immutable
data class AppTheme(
    val material: ColorScheme,
    val activityBackground: Color,
    val sheetBackground: Color,
    val menuLighter: MenuTheme,
    val menuDarker: MenuTheme,
    val workspaceSidebar: WorkspaceSidebarTheme,
    val chatBottomBar: ChatBottomBarTheme,
    val chat: ChatTheme,
    val connectionError: ConnectionErrorTheme,
    val actionBar: ActionBarTheme,
    val workspaces: WorkspacesTheme,
    val dialog: DialogTheme,
    val promptEditor: PromptEditorTheme,
    val appConfig: AppConfigTheme
) {

    @Composable
    fun textFieldColors() = TextFieldDefaults.colors().copy(
        unfocusedContainerColor = material.background,
        focusedContainerColor = material.background,
        focusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )
}

@Immutable
data class MenuTheme(
    val headerForeground: Color,
    val itemBackground: Color,
    val itemForeground: Color,
    val itemForegroundSecondary: Color,
    val itemForegroundChevron: Color,
    val divider: Color,
    val buttonBackground: Color,
    val buttonForeground: Color,
) {
    val headerText = TextStyles.mono.copy(
        color = headerForeground,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.65.sp,
        fontSize = 13.sp
    )

    val itemText = TextStyles.base.copy(
        color = itemForeground,
        fontWeight = FontWeight.W400,
        fontSize = 17.sp
    )

    val itemSecondaryText = TextStyles.base.copy(
        color = itemForegroundSecondary,
        fontWeight = FontWeight.W400,
        fontSize = 17.sp
    )

    val subtitle = TextStyles.base.copy(
        color = itemForegroundSecondary,
        fontWeight = FontWeight.W400,
        fontSize = 13.sp
    )

    val buttonText = TextStyles.base.copy(
        color = buttonForeground,
        fontWeight = FontWeight.W600,
        fontSize = 17.sp
    )

    val dividerThickness = 1.dp
}

@Immutable
data class WorkspaceSidebarTheme(
    val background: Color,
    val headerForeground: Color,
    val itemForeground: Color,
    val chatItemBackgroundSelected: Color,
    val divider: Color,
    val iconForeground: Color
) {
    val titleText = TextStyles.base.copy(
        color = itemForeground,
        fontWeight = FontWeight.W500,
        fontSize = 17.sp
    )

    val headerText = TextStyles.mono.copy(
        color = headerForeground,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.65.sp,
        fontSize = 13.sp
    )

    val chatItemText = TextStyles.base.copy(
        color = itemForeground,
        fontWeight = FontWeight.W400,
        fontSize = 17.sp
    )

    val actionText = TextStyles.base.copy(
        color = itemForeground,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp
    )
}

@Immutable
data class ChatBottomBarTheme(
    val background: Color,
    val foreground: Color,
    val boxBorder: Color,
    val boxHint: Color,
    val toggleOnForeground: Color,
    val toggleOnBackground: Color,
    val toggleOffForeground: Color,
    val toggleOffBackground: Color,
) {
    val boxText = TextStyles.base.copy(
        color = foreground,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp
    )

    val boxHintText = TextStyles.base.copy(
        color = boxHint,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp
    )
}

@Immutable
data class ChatTheme(
    val foreground: Color,
    val bubbleBackgroundUser: Color,
    val avatarBorderBot: Color,
    val statusBackgroundNeutral: Color,
    val statusBackgroundInCall: Color,
    val statusBackgroundError: Color,
    val statusForeground: Color,
    val placeholderTextColor: Color,
    val markdownColorsBot: MarkdownColors,
) {
    val text = TextStyles.base.copy(
        color = foreground,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp
    )

    val statusText = TextStyles.base.copy(
        color = foreground,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp
    )

    val placeholderText = TextStyles.base.copy(
        color = placeholderTextColor,
        fontWeight = FontWeight.W800,
        fontSize = 18.sp
    )

    val markdownTypography = DefaultMarkdownTypography(
        h1 = TextStyles.base.copy(fontSize = 20.sp, fontWeight = FontWeight.W700),
        h2 = TextStyles.base.copy(fontSize = 18.sp, fontWeight = FontWeight.W700),
        h3 = TextStyles.base.copy(fontSize = 18.sp, fontWeight = FontWeight.W600),
        h4 = TextStyles.base.copy(fontSize = 16.sp, fontWeight = FontWeight.W700),
        h5 = TextStyles.base.copy(fontSize = 16.sp, fontWeight = FontWeight.W600),
        h6 = TextStyles.base.copy(fontSize = 15.sp, fontWeight = FontWeight.W700),
        text = text,
        code = TextStyles.mono.copy(fontSize = 14.sp, fontWeight = FontWeight.W400),
        quote = text,
        paragraph = text,
        ordered = text,
        bullet = text,
        list = text
    )
}

@Immutable
data class ConnectionErrorTheme(
    val errorIcon: Color,
    val errorTextColor: Color,
    val errorButton: Color,
    val errorButtonTextColor: Color,
) {
    val errorTitle = TextStyles.base.copy(
        color = errorTextColor,
        fontWeight = FontWeight.W500,
        fontSize = 22.sp
    )

    val errorMessage = TextStyles.mono.copy(
        color = errorTextColor,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp
    )

    val errorButtonText = TextStyles.base.copy(
        color = errorButtonTextColor,
        fontWeight = FontWeight.W700,
        fontSize = 16.sp
    )
}

@Immutable
data class ActionBarTheme(
    val background: Color,
    val foreground: Color,
    val secondaryForeground: Color,
) {
    val title = TextStyles.base.copy(
        color = foreground,
        fontWeight = FontWeight.W600,
        fontSize = 19.sp
    )
}

@Immutable
data class WorkspacesTheme(
    val foreground: Color,
    val background: Color,
    val itemBackground: Color,
    val foregroundSecondary: Color,
    val foregroundTertiary: Color,
    val editButton: Color,
    val defaultIconBackground: Color,
    val createWorkspaceIcon: Color,
) {
    val sortButtonText = TextStyles.base.copy(
        color = foreground,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp
    )

    val header = TextStyles.base.copy(
        color = foreground,
        fontWeight = FontWeight.W600,
        fontSize = 23.sp
    )

    val itemTitle = TextStyles.base.copy(
        color = foreground,
        fontWeight = FontWeight.W600,
        fontSize = 17.sp
    )

    val itemKey = TextStyles.mono.copy(
        color = foregroundSecondary,
        fontWeight = FontWeight.W500,
        fontSize = 11.sp
    )

    val itemValue = TextStyles.mono.copy(
        color = foregroundTertiary,
        fontWeight = FontWeight.W400,
        fontSize = 11.sp
    )

    val createWorkspaceText = TextStyles.base.copy(
        color = foreground,
        fontWeight = FontWeight.W600,
        fontSize = 17.sp
    )
}

@Immutable
data class DialogTheme(
    val background: Color,
    val border: Color,
    val titleColor: Color,
    val messageColor: Color,
    val textInputTextColor: Color,
    val textInputBackground: Color,
    val textInputBorder: Color,
    val buttonTextColor: Color,
    val buttonBackground: Color
) {
    val title = TextStyles.base.copy(
        color = titleColor,
        fontWeight = FontWeight.W700,
        fontSize = 16.sp
    )
    val message = TextStyles.base.copy(
        color = titleColor,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp
    )

    val textInputText = TextStyles.base.copy(
        color = titleColor,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp
    )

    val buttonText = TextStyles.base.copy(
        color = buttonTextColor,
        fontWeight = FontWeight.W700,
        fontSize = 14.sp
    )
}

@Immutable
data class PromptEditorTheme(
    val roleColor: Color,
    val roleSpinnerColor: Color,
    val textInputBorder: Color,
    val textInputBackground: Color,
    val textInputTextColor: Color,
    val addContextIcon: Color,
    val addContextTextColor: Color,
) {
    val role = TextStyles.mono.copy(
        color = roleColor,
        fontWeight = FontWeight.W500,
        fontSize = 13.sp
    )

    val textInputText = TextStyles.base.copy(
        color = textInputTextColor,
        fontWeight = FontWeight.W400,
        fontSize = 13.sp
    )

    val addContextText = TextStyles.base.copy(
        color = addContextTextColor,
        fontWeight = FontWeight.W600,
        fontSize = 17.sp
    )
}

@Immutable
data class AppConfigTheme(
    val foreground: Color,
    val foregroundLink: Color,
    val foregroundSecondary: Color,
    val textFieldBackground: Color,
    val textFieldBorder: Color,
    val textFieldForeground: Color,
    val buttonForeground: Color,
    val buttonBackground: Color,
) {
    val bodyText = FormattedTextStyle(
        base = TextStyles.base.copy(
            color = foreground,
            fontWeight = FontWeight.W400,
            fontSize = 14.sp
        ),
        boldColor = foregroundLink,
        linkColor = foregroundLink
    )

    val bodyTextHint = TextStyles.base.copy(
        color = foregroundSecondary,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp
    )

    val fieldHeader = TextStyles.base.copy(
        color = foreground,
        fontWeight = FontWeight.W700,
        fontSize = 16.sp
    )

    val textFieldText = TextStyles.base.copy(
        color = textFieldForeground,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp
    )

    val buttonText = TextStyles.base.copy(
        color = buttonForeground,
        fontWeight = FontWeight.W600,
        fontSize = 17.sp
    )
}

val AppThemeDark = AppTheme(
    material = Colors.darkMaterialScheme,
    activityBackground = Colors.black,
    sheetBackground = Colors.neutral900,
    menuLighter = MenuTheme(
        headerForeground = Colors.neutral400,
        itemBackground = Colors.neutral700,
        itemForeground = Colors.white,
        itemForegroundSecondary = Colors.neutral400,
        itemForegroundChevron = Colors.neutral500,
        divider = Colors.neutral600,
        buttonBackground = Colors.white,
        buttonForeground = Colors.black,
    ),
    menuDarker = MenuTheme(
        headerForeground = Colors.neutral400,
        itemBackground = Colors.neutral800,
        itemForeground = Colors.white,
        itemForegroundSecondary = Colors.neutral400,
        itemForegroundChevron = Colors.neutral500,
        divider = Color(0xFF3B3B3B),
        buttonBackground = Colors.white,
        buttonForeground = Colors.black,
    ),
    workspaceSidebar = WorkspaceSidebarTheme(
        headerForeground = Colors.neutral400,
        itemForeground = Colors.white,
        chatItemBackgroundSelected = Colors.neutral800,
        divider = Colors.neutral700,
        background = Colors.black,
        iconForeground = Colors.white
    ),
    chatBottomBar = ChatBottomBarTheme(
        background = Colors.black,
        foreground = Colors.white,
        boxBorder = Colors.neutral600,
        boxHint = Colors.neutral500,
        toggleOnForeground = Colors.black,
        toggleOnBackground = Colors.white,
        toggleOffBackground = Colors.neutral800,
        toggleOffForeground = Colors.neutral400
    ),
    chat = ChatTheme(
        foreground = Colors.white,
        bubbleBackgroundUser = Colors.neutral800,
        avatarBorderBot = Colors.neutral600,
        statusForeground = Colors.white,
        statusBackgroundNeutral = Colors.neutral700,
        statusBackgroundInCall = Colors.greenDark,
        statusBackgroundError = Colors.errorContainerDark,
        placeholderTextColor = Colors.neutral500,
        markdownColorsBot = DefaultMarkdownColors(
            text = Colors.white,
            codeText = Colors.neutral200,
            inlineCodeText = Colors.neutral200,
            linkText = Colors.greenLight,
            codeBackground = Colors.neutral800,
            inlineCodeBackground = Colors.neutral800,
            dividerColor = Colors.neutral700
        )
    ),
    connectionError = ConnectionErrorTheme(
        errorIcon = Colors.errorDark,
        errorTextColor = Colors.white,
        errorButton = Colors.neutral800,
        errorButtonTextColor = Colors.white
    ),
    actionBar = ActionBarTheme(
        background = Colors.black,
        foreground = Colors.white,
        secondaryForeground = Colors.neutral500
    ),
    workspaces = WorkspacesTheme(
        foreground = Colors.white,
        background = Colors.black,
        itemBackground = Colors.neutral800,
        foregroundSecondary = Colors.neutral300,
        foregroundTertiary = Colors.neutral400,
        editButton = Colors.neutral600,
        defaultIconBackground = Color(0xFFA78BFA),
        createWorkspaceIcon = Colors.neutral400
    ),
    dialog = DialogTheme(
        background = Colors.neutral700,
        border = Colors.neutral700,
        titleColor = Colors.white,
        messageColor = Colors.neutral200,
        textInputTextColor = Colors.neutral100,
        buttonTextColor = Colors.neutral200,
        buttonBackground = Colors.neutral800,
        textInputBackground = Colors.neutral600,
        textInputBorder = Colors.neutral500
    ),
    promptEditor = PromptEditorTheme(
        roleColor = Colors.neutral400,
        roleSpinnerColor = Colors.neutral500,
        textInputBackground = Colors.neutral800,
        textInputBorder = Colors.neutral600,
        textInputTextColor = Colors.neutral300,
        addContextIcon = Colors.neutral400,
        addContextTextColor = Colors.white
    ),
    appConfig = AppConfigTheme(
        foreground = Colors.white,
        foregroundLink = Colors.neutral200,
        foregroundSecondary = Colors.neutral400,
        textFieldBackground = Colors.neutral800,
        textFieldBorder = Colors.neutral600,
        textFieldForeground = Colors.neutral300,
        buttonForeground = Colors.black,
        buttonBackground = Colors.white
    )
)

val AppThemeLight = AppTheme(
    material = Colors.lightMaterialScheme,
    activityBackground = Colors.white,
    sheetBackground = Colors.neutral100,
    menuLighter = MenuTheme(
        headerForeground = Colors.neutral600,
        itemBackground = Colors.neutral200,
        itemForeground = Colors.black,
        itemForegroundSecondary = Colors.neutral600,
        itemForegroundChevron = Colors.neutral500,
        divider = Colors.neutral300,
        buttonBackground = Colors.black,
        buttonForeground = Colors.white,
    ),
    menuDarker = MenuTheme(
        headerForeground = Colors.neutral600,
        itemBackground = Colors.neutral100,
        itemForeground = Colors.black,
        itemForegroundSecondary = Colors.neutral600,
        itemForegroundChevron = Colors.neutral500,
        divider = Colors.neutral200,
        buttonBackground = Colors.black,
        buttonForeground = Colors.white,
    ),
    workspaceSidebar = WorkspaceSidebarTheme(
        headerForeground = Colors.neutral600,
        itemForeground = Colors.black,
        chatItemBackgroundSelected = Colors.neutral200,
        divider = Colors.neutral300,
        background = Colors.white,
        iconForeground = Colors.black
    ),
    chatBottomBar = ChatBottomBarTheme(
        background = Colors.white,
        foreground = Colors.black,
        boxBorder = Colors.neutral400,
        boxHint = Colors.neutral500,
        toggleOnForeground = Colors.white,
        toggleOnBackground = Colors.black,
        toggleOffBackground = Colors.neutral200,
        toggleOffForeground = Colors.neutral600
    ),
    chat = ChatTheme(
        foreground = Colors.black,
        bubbleBackgroundUser = Colors.neutral200,
        avatarBorderBot = Colors.neutral400,
        statusForeground = Colors.black,
        statusBackgroundNeutral = Colors.neutral300,
        statusBackgroundInCall = Colors.greenLight,
        statusBackgroundError = Colors.errorDark,
        placeholderTextColor = Colors.neutral500,
        markdownColorsBot = DefaultMarkdownColors(
            text = Colors.black,
            codeText = Colors.neutral800,
            inlineCodeText = Colors.neutral800,
            linkText = Colors.greenDark,
            codeBackground = Colors.neutral200,
            inlineCodeBackground = Colors.neutral200,
            dividerColor = Colors.neutral300
        )
    ),
    connectionError = ConnectionErrorTheme(
        errorIcon = Colors.errorContainerDark,
        errorTextColor = Colors.black,
        errorButton = Colors.neutral200,
        errorButtonTextColor = Colors.black
    ),
    actionBar = ActionBarTheme(
        background = Colors.white,
        foreground = Colors.black,
        secondaryForeground = Colors.neutral500
    ),
    workspaces = WorkspacesTheme(
        foreground = Colors.black,
        background = Colors.white,
        itemBackground = Colors.neutral100,
        foregroundSecondary = Colors.neutral700,
        foregroundTertiary = Colors.neutral600,
        editButton = Colors.neutral400,
        defaultIconBackground = Color(0xFFA78BFA),
        createWorkspaceIcon = Colors.neutral600
    ),
    dialog = DialogTheme(
        background = Colors.neutral300,
        border = Colors.neutral300,
        titleColor = Colors.black,
        messageColor = Colors.neutral800,
        textInputTextColor = Colors.neutral900,
        buttonTextColor = Colors.neutral800,
        buttonBackground = Colors.neutral200,
        textInputBackground = Colors.neutral200,
        textInputBorder = Colors.neutral400
    ),
    promptEditor = PromptEditorTheme(
        roleColor = Colors.neutral600,
        roleSpinnerColor = Colors.neutral500,
        textInputBackground = Colors.neutral200,
        textInputBorder = Colors.neutral400,
        textInputTextColor = Colors.neutral700,
        addContextIcon = Colors.neutral600,
        addContextTextColor = Colors.black
    ),
    appConfig = AppConfigTheme(
        foreground = Colors.black,
        foregroundLink = Colors.neutral800,
        foregroundSecondary = Colors.neutral600,
        textFieldBackground = Colors.neutral100,
        textFieldBorder = Colors.neutral300,
        textFieldForeground = Colors.neutral700,
        buttonForeground = Colors.white,
        buttonBackground = Colors.black
    )
)

val LocalAppTheme = staticCompositionLocalOf { AppThemeDark }