package co.daily.opensesame.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import co.daily.opensesame.Preferences
import co.daily.opensesame.Theme
import co.daily.opensesame.api.CachingDataApi
import co.daily.opensesame.api.DataApiPreview
import co.daily.opensesame.api.DataApiRestApi
import co.daily.opensesame.ui.theme.Colors
import co.daily.opensesame.ui.theme.RTVIClientTheme

@Composable
fun AppContext(content: @Composable () -> Unit) {
    
    val theme = Preferences.theme.value?.let(Theme.Companion::fromStr) ?: Theme.System

    val lightMode = when(theme) {
        Theme.Light -> true
        Theme.Dark -> false
        Theme.System -> !isSystemInDarkTheme()
    }
    
    RTVIClientTheme(lightMode = lightMode) {

        val baseUrl = Preferences.backendUrl.value
        val apiKey = Preferences.apiKey.value

        CompositionLocalProvider(
            LocalDataApi provides CachingDataApi(
                DataApiRestApi(
                    baseUrl = baseUrl,
                    apiKey = apiKey
                )
            ),
            content = content
        )
    }
}

@Composable
fun AppContextPreview(content: @Composable BoxScope.() -> Unit) {
    RTVIClientTheme(lightMode = false) {
        CompositionLocalProvider(
            LocalDataApi provides CachingDataApi(DataApiPreview())
        ) {
            Box(modifier = Modifier.background(Colors.black), content = content)
        }
    }
}

val LocalDataApi =
    compositionLocalOf<CachingDataApi> { throw Exception("Data API not specified") }
