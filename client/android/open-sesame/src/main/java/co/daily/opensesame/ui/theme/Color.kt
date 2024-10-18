package co.daily.opensesame.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

object Colors {
    val logoBorder = Color(0xFFE2E8F0)

    val neutral50 = Color(0xFFFAFAFA)
    val neutral100 = Color(0xFFF5F5F5)
    val neutral200 = Color(0xFFE5E5E5)
    val neutral300 = Color(0xFFD4D4D4)
    val neutral400 = Color(0xFFA3A3A3)
    val neutral500 = Color(0xFF737373)
    val neutral600 = Color(0xFF525252)
    val neutral700 = Color(0xFF404040)
    val neutral800 = Color(0xFF262626)
    val neutral900 = Color(0xFF171717)
    val neutral950 = Color(0xFF0A0A0A)

    val white = Color.White
    val black = Color.Black

    val greenLight = Color(0xFF10B981)
    val greenDark = Color(0xFF022C22)

    // Auto generated
    // https://material-foundation.github.io/material-theme-builder/

    val primaryDark = Color(0xFF4EDEA3)
    val onPrimaryDark = Color(0xFF003824)
    val primaryContainerDark = Color(0xFF00B07A)
    val onPrimaryContainerDark = Color(0xFF000D06)
    val secondaryDark = Color(0xFF9ED2B5)
    val onSecondaryDark = Color(0xFF013824)
    val secondaryContainerDark = Color(0xFF164833)
    val onSecondaryContainerDark = Color(0xFFACE0C3)
    val tertiaryDark = Color(0xFFBEC2FF)
    val onTertiaryDark = Color(0xFF192184)
    val tertiaryContainerDark = Color(0xFF8A93F7)
    val onTertiaryContainerDark = Color(0xFF00023E)
    val errorDark = Color(0xFFFFB4AB)
    val onErrorDark = Color(0xFF690005)
    val errorContainerDark = Color(0xFF93000A)
    val onErrorContainerDark = Color(0xFFFFDAD6)
    val inversePrimaryDark = Color(0xFF006C49)

    val darkMaterialScheme = darkColorScheme(
        primary = primaryDark,
        onPrimary = onPrimaryDark,
        primaryContainer = primaryContainerDark,
        onPrimaryContainer = onPrimaryContainerDark,
        secondary = secondaryDark,
        onSecondary = onSecondaryDark,
        secondaryContainer = secondaryContainerDark,
        onSecondaryContainer = onSecondaryContainerDark,
        tertiary = tertiaryDark,
        onTertiary = onTertiaryDark,
        tertiaryContainer = tertiaryContainerDark,
        onTertiaryContainer = onTertiaryContainerDark,
        error = errorDark,
        onError = onErrorDark,
        errorContainer = errorContainerDark,
        onErrorContainer = onErrorContainerDark,
        background = neutral950,
        onBackground = neutral300,
        surface = neutral900,
        onSurface = neutral300,
        surfaceVariant = neutral700,
        onSurfaceVariant = neutral400,
        outline = neutral500,
        outlineVariant = neutral700,
        scrim = black,
        inverseSurface = neutral300,
        inverseOnSurface = neutral800,
        inversePrimary = inversePrimaryDark,
        surfaceDim = neutral900,
        surfaceBright = neutral800,
        surfaceContainerLowest = neutral950,
        surfaceContainerLow = neutral900,
        surfaceContainer = neutral900,
        surfaceContainerHigh = neutral800,
        surfaceContainerHighest = neutral800,
    )

    val lightMaterialScheme = darkColorScheme(
        primary = primaryDark,
        onPrimary = onPrimaryDark,
        primaryContainer = primaryContainerDark,
        onPrimaryContainer = onPrimaryContainerDark,
        secondary = secondaryDark,
        onSecondary = onSecondaryDark,
        secondaryContainer = secondaryContainerDark,
        onSecondaryContainer = onSecondaryContainerDark,
        tertiary = tertiaryDark,
        onTertiary = onTertiaryDark,
        tertiaryContainer = tertiaryContainerDark,
        onTertiaryContainer = onTertiaryContainerDark,
        error = errorDark,
        onError = onErrorDark,
        errorContainer = errorContainerDark,
        onErrorContainer = onErrorContainerDark,
        background = neutral50,
        onBackground = neutral700,
        surface = neutral100,
        onSurface = neutral700,
        surfaceVariant = neutral300,
        onSurfaceVariant = neutral600,
        outline = neutral500,
        outlineVariant = neutral300,
        scrim = white,
        inverseSurface = neutral700,
        inverseOnSurface = neutral200,
        inversePrimary = inversePrimaryDark,
        surfaceDim = neutral100,
        surfaceBright = neutral200,
        surfaceContainerLowest = neutral50,
        surfaceContainerLow = neutral100,
        surfaceContainer = neutral100,
        surfaceContainerHigh = neutral200,
        surfaceContainerHighest = neutral200,
    )
}