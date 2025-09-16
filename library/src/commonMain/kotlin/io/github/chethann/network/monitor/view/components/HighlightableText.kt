package io.github.chethann.network.monitor.view.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit

@Composable
fun HighlightableText(
    text: String,
    searchQuery: String,
    highlightColor: Color,
    textColor: Color = MaterialTheme.colors.onSurface,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    modifier: Modifier = Modifier
) {
    if (searchQuery.isEmpty()) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            modifier = modifier
        )
    } else {
        val annotatedString = buildAnnotatedString {
            var lastIndex = 0
            var startIndex = text.indexOf(searchQuery, ignoreCase = true)

            while (startIndex != -1) {
                // Add text before highlight
                if (startIndex > lastIndex) {
                    append(text.substring(lastIndex, startIndex))
                }

                // Add highlighted text with background color
                withStyle(style = SpanStyle(background = highlightColor)) {
                    append(text.substring(startIndex, startIndex + searchQuery.length))
                }

                lastIndex = startIndex + searchQuery.length
                startIndex = text.indexOf(searchQuery, lastIndex, ignoreCase = true)
            }

            // Add remaining text
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }

        Text(
            text = annotatedString,
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            modifier = modifier
        )
    }
}
