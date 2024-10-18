package co.daily.opensesame.ui.theme

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import co.daily.opensesame.R

object TextStyles {
    val base = TextStyle(fontFamily = FontFamily(Font(R.font.dm_sans)))
    val mono = TextStyle(fontFamily = FontFamily(Font(R.font.dm_mono_medium)))
}

@Composable
fun TextStyle.StyledText(
    text: String,
    modifier: Modifier = Modifier,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign = TextAlign.Left,
    color: Color = this.color,
) {
    Text(
        modifier = modifier,
        text = text,
        style = this,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        overflow = overflow,
        maxLines = maxLines,
        textAlign = textAlign
    )
}