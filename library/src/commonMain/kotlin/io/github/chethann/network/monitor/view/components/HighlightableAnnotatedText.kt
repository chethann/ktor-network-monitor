package io.github.chethann.network.monitor.view.components

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit

@Composable
fun HighlightableAnnotatedText(
    annotatedText: AnnotatedString,
    searchQuery: String,
    highlightColor: Color,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontFamily: FontFamily? = null,
    modifier: Modifier = Modifier
) {
    if (searchQuery.isEmpty()) {
        Text(
            text = annotatedText,
            fontSize = fontSize,
            fontFamily = fontFamily,
            modifier = modifier
        )
    } else {
        // Create a new AnnotatedString with highlights while preserving existing styles
        val highlightedString = buildAnnotatedString {
            val plainText = annotatedText.text
            var lastIndex = 0
            var startIndex = plainText.indexOf(searchQuery, ignoreCase = true)

            while (startIndex != -1) {
                // Add text before highlight with original styling
                if (startIndex > lastIndex) {
                    val segment = annotatedText.subSequence(lastIndex, startIndex)
                    append(segment)
                }

                // Add highlighted text with original styling + highlight background
                val highlightSegment = annotatedText.subSequence(startIndex, startIndex + searchQuery.length)
                withStyle(style = SpanStyle(background = highlightColor)) {
                    append(highlightSegment)
                }

                lastIndex = startIndex + searchQuery.length
                startIndex = plainText.indexOf(searchQuery, lastIndex, ignoreCase = true)
            }

            // Add remaining text with original styling
            if (lastIndex < plainText.length) {
                val remainingSegment = annotatedText.subSequence(lastIndex, plainText.length)
                append(remainingSegment)
            }
        }

        Text(
            text = highlightedString,
            fontSize = fontSize,
            fontFamily = fontFamily,
            modifier = modifier
        )
    }
}
