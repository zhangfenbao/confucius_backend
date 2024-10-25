package co.daily.opensesame.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier

@Composable
fun <E> Modifier.applyIfNotNull(value: E?, action: @Composable Modifier.(E) -> Modifier): Modifier {
    return action(value ?: return this)
}
@Composable
fun Modifier.applyIf(condition: Boolean, action: @Composable Modifier.() -> Modifier): Modifier {
    return if (condition) action(this) else this
}

@JvmInline
@Immutable
value class ImmutableMarker<E>(val value: E)