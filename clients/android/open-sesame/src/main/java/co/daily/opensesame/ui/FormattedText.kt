package co.daily.opensesame.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import java.util.concurrent.atomic.AtomicInteger

interface FormattedTextScope {
    fun text(text: String)
    fun bold(text: String)
    fun link(text: String, url: String)
    fun link(text: String, action: () -> Unit)
}

@Immutable
data class FormattedTextStyle(
    val base: TextStyle,
    val boldColor: Color,
    val boldWeight: FontWeight = FontWeight.W700,
    val linkColor: Color,
    val linkWeight: FontWeight = FontWeight.W400,
    val linkUnderline: Boolean = true
)

@Composable
fun FormattedTextStyle.FormattedText(
    modifier: Modifier = Modifier,
    builder: FormattedTextScope.() -> Unit
) {
    val actions = HashMap<String, () -> Unit>()

    val annotatedString = buildAnnotatedString {

        val nextId = AtomicInteger(1)

        builder(object : FormattedTextScope {
            override fun text(text: String) {
                append(text)
            }

            override fun bold(text: String) {
                withStyle(
                    style = SpanStyle(
                        color = boldColor,
                        fontWeight = boldWeight
                    )
                ) {
                    append(text)
                }
            }

            override fun link(text: String, url: String) {
                pushStringAnnotation(tag = "url", annotation = url)
                withStyle(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = if (linkUnderline) TextDecoration.Underline else TextDecoration.None
                    )
                ) {
                    append(text)
                }
                pop()
            }

            override fun link(text: String, action: () -> Unit) {
                val id = nextId.getAndIncrement().toString()

                actions[id] = action

                pushStringAnnotation(tag = "action", annotation = id)
                withStyle(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = if (linkUnderline) TextDecoration.Underline else TextDecoration.None
                    )
                ) {
                    append(text)
                }
                pop()
            }
        })
    }

    val context = LocalContext.current

    ClickableText(
        modifier = modifier,
        text = annotatedString,
        style = base,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "url", start = offset, end = offset)
                .firstOrNull()?.let {
                    openUrlInBrowser(context, it.item)
                }

            annotatedString.getStringAnnotations(tag = "action", start = offset, end = offset)
                .firstOrNull()?.let {
                    actions[it.item]?.let { it() }
                }
        }
    )
}

private fun openUrlInBrowser(context: Context, url: String) {
    Intent(Intent.ACTION_VIEW).apply {
        setData(Uri.parse(url))
        context.startActivity(this)
    }
}