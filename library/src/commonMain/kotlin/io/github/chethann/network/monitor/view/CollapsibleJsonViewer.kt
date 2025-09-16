package io.github.chethann.network.monitor.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.chethann.network.monitor.view.components.HighlightableAnnotatedText
import io.github.chethann.network.monitor.view.components.HighlightableText
import io.github.chethann.network.monitor.view.theme.extendedColors
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class JsonNode(
    val key: String? = null,
    val value: JsonElement,
    val level: Int = 0,
    val isLast: Boolean = true
)

@Composable
fun CollapsibleJsonViewer(
    jsonContent: String,
    searchQuery: String = "",
    highlightColor: Color? = null,
    modifier: Modifier = Modifier
) {
    if (jsonContent.isBlank()) {
        Text(
            text = "No content",
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            modifier = modifier
        )
        return
    }

    // Extract JSON content (handle cases with headers like "BODY Content-Type: application/json")
    val extractedJsonContent = remember(jsonContent) {
        extractJsonFromContent(jsonContent)
    }

    // Parse JSON outside of composable context
    val jsonElement = remember(extractedJsonContent) {
        try {
            if (extractedJsonContent.isNotBlank()) {
                Json.parseToJsonElement(extractedJsonContent)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    val effectiveHighlightColor = highlightColor ?: MaterialTheme.colors.secondary.copy(alpha = 0.35f)

    Column(modifier = modifier) {
        if (jsonElement != null) {
            JsonElementComposable(
                node = JsonNode(value = jsonElement),
                searchQuery = searchQuery,
                highlightColor = effectiveHighlightColor
            )
        } else {
            // If not valid JSON, show as plain text with better formatting
            HighlightableText(
                text = formatNonJsonContent(jsonContent),
                searchQuery = searchQuery,
                highlightColor = effectiveHighlightColor,
                textColor = MaterialTheme.colors.onSurface,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
            )
        }
    }
}

// Extract JSON content from text that might contain headers
private fun extractJsonFromContent(content: String): String {
    val trimmed = content.trim()

    // Look for JSON patterns - content that starts with { or [
    val jsonStartIndex = trimmed.indexOfFirst { it == '{' || it == '[' }
    if (jsonStartIndex == -1) return ""

    // Extract everything from the first JSON character
    val potentialJson = trimmed.substring(jsonStartIndex)

    // Try to find the matching closing brace/bracket
    return findCompleteJsonContent(potentialJson) ?: ""
}

private fun findCompleteJsonContent(content: String): String? {
    var braceCount = 0
    var bracketCount = 0
    var inString = false
    var escaped = false

    for (i in content.indices) {
        val char = content[i]

        if (escaped) {
            escaped = false
            continue
        }

        when (char) {
            '\\' -> escaped = true
            '"' -> inString = !inString
            '{' -> if (!inString) braceCount++
            '}' -> if (!inString) braceCount--
            '[' -> if (!inString) bracketCount++
            ']' -> if (!inString) bracketCount--
        }

        // If we've closed all braces and brackets, we have complete JSON
        if (braceCount == 0 && bracketCount == 0 && i > 0) {
            return content.substring(0, i + 1)
        }
    }

    return content // Return as is if we can't find proper closing
}

private fun formatNonJsonContent(content: String): String {
    // Format non-JSON content for better readability
    return content.lines().joinToString("\n") { line ->
        val trimmed = line.trim()
        when {
            trimmed.startsWith("BODY ") -> "📄 ${trimmed.substring(5)}"
            trimmed.contains(": ") && !trimmed.startsWith("{") && !trimmed.startsWith("[") ->
                "• $trimmed"
            else -> trimmed
        }
    }
}

// Get arrow color based on nesting level
@Composable
private fun getArrowColor(level: Int): Color = when (level % 6) {
    0 -> MaterialTheme.colors.primary
    1 -> MaterialTheme.colors.secondary
    2 -> MaterialTheme.colors.primaryVariant
    3 -> MaterialTheme.colors.error
    4 -> MaterialTheme.colors.onSurface.copy(alpha = 0.75f)
    5 -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
    else -> MaterialTheme.colors.primary
}

// Get key color based on nesting level
@Composable
private fun getKeyColor(level: Int): Color = when (level % 6) {
    0 -> MaterialTheme.colors.primary
    1 -> MaterialTheme.colors.secondary
    2 -> MaterialTheme.colors.primaryVariant
    3 -> MaterialTheme.colors.error
    4 -> MaterialTheme.colors.onSurface.copy(alpha = 0.75f)
    5 -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
    else -> MaterialTheme.colors.primary
}

@Composable
private fun JsonElementComposable(
    node: JsonNode,
    searchQuery: String,
    highlightColor: Color,
    isExpanded: Boolean = true
) {
    var expanded by remember { mutableStateOf(isExpanded) }
    when (val element = node.value) {
        is JsonObject -> JsonObjectComposable(
            node = node,
            element = element,
            expanded = expanded,
            onToggle = { expanded = !expanded },
            searchQuery = searchQuery,
            highlightColor = highlightColor
        )
        is JsonArray -> JsonArrayComposable(
            node = node,
            element = element,
            expanded = expanded,
            onToggle = { expanded = !expanded },
            searchQuery = searchQuery,
            highlightColor = highlightColor
        )
        is JsonPrimitive -> JsonPrimitiveComposable(
            node = node,
            element = element,
            searchQuery = searchQuery,
            highlightColor = highlightColor
        )
    }
}

@Composable
private fun JsonObjectComposable(
    node: JsonNode,
    element: JsonObject,
    expanded: Boolean,
    onToggle: () -> Unit,
    searchQuery: String,
    highlightColor: Color
) {
    val indent = "  ".repeat(node.level)
    val keyText = node.key?.let { "\"$it\": " } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Expand/Collapse arrow
        Text(
            text = if (expanded) "▼" else "▶",
            color = getArrowColor(node.level),
            fontSize = 12.sp,
            modifier = Modifier.padding(end = 4.dp)
        )

        // Indent + Key + Opening brace
        val openingText = buildAnnotatedString {
            append(indent)
            if (keyText.isNotEmpty()) {
                withStyle(SpanStyle(color = getKeyColor(node.level), fontWeight = FontWeight.Bold)) {
                    append(keyText)
                }
            }
            withStyle(SpanStyle(color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f))) {
                append("{")
            }
            if (!expanded) {
                withStyle(SpanStyle(color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                    append(" ... } (${element.size} ${if (element.size == 1) "item" else "items"})")
                }
            }
        }

        HighlightableAnnotatedText(
            annotatedText = openingText,
            searchQuery = searchQuery,
            highlightColor = highlightColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
    }

    if (expanded) {
        Column {
            element.entries.forEachIndexed { index, (key, value) ->
                val isLast = index == element.size - 1
                JsonElementComposable(
                    node = JsonNode(
                        key = key,
                        value = value,
                        level = node.level + 1,
                        isLast = isLast
                    ),
                    searchQuery = searchQuery,
                    highlightColor = highlightColor
                )
            }

            // Closing brace
            Row {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "${"  ".repeat(node.level)}}${if (!node.isLast) "," else ""}",
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun JsonArrayComposable(
    node: JsonNode,
    element: JsonArray,
    expanded: Boolean,
    onToggle: () -> Unit,
    searchQuery: String,
    highlightColor: Color
) {
    val indent = "  ".repeat(node.level)
    val keyText = node.key?.let { "\"$it\": " } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Expand/Collapse arrow
        Text(
            text = if (expanded) "▼" else "▶",
            color = getArrowColor(node.level),
            fontSize = 12.sp,
            modifier = Modifier.padding(end = 4.dp)
        )

        // Indent + Key + Opening bracket
        val openingText = buildAnnotatedString {
            append(indent)
            if (keyText.isNotEmpty()) {
                withStyle(SpanStyle(color = getKeyColor(node.level), fontWeight = FontWeight.Bold)) {
                    append(keyText)
                }
            }
            withStyle(SpanStyle(color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f))) {
                append("[")
            }
            if (!expanded) {
                withStyle(SpanStyle(color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                    append(" ... ] (${element.size} ${if (element.size == 1) "item" else "items"})")
                }
            }
        }

        HighlightableAnnotatedText(
            annotatedText = openingText,
            searchQuery = searchQuery,
            highlightColor = highlightColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
    }

    if (expanded) {
        Column {
            element.forEachIndexed { index, value ->
                val isLast = index == element.size - 1
                JsonElementComposable(
                    node = JsonNode(
                        value = value,
                        level = node.level + 1,
                        isLast = isLast
                    ),
                    searchQuery = searchQuery,
                    highlightColor = highlightColor
                )
            }

            // Closing bracket
            Row {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "${"  ".repeat(node.level)}]${if (!node.isLast) "," else ""}",
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun JsonPrimitiveComposable(
    node: JsonNode,
    element: JsonPrimitive,
    searchQuery: String,
    highlightColor: Color
) {
    val indent = "  ".repeat(node.level)
    val keyText = node.key?.let { "\"$it\": " } ?: ""

    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Spacer(modifier = Modifier.width(16.dp)) // Space for arrow alignment

        val valueText = buildAnnotatedString {
            append(indent)
            if (keyText.isNotEmpty()) {
                withStyle(SpanStyle(color = getKeyColor(node.level), fontWeight = FontWeight.Bold)) {
                    append(keyText)
                }
            }

            // Color code different value types
            val (value, color) = when {
                element.isString -> "\"${element.content}\"" to MaterialTheme.extendedColors.success
                element.content == "null" -> "null" to MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                element.content == "true" || element.content == "false" -> element.content to MaterialTheme.extendedColors.info
                else -> element.content to MaterialTheme.extendedColors.warning
            }

            withStyle(SpanStyle(color = color)) {
                append(value)
            }

            if (!node.isLast) {
                withStyle(SpanStyle(color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f))) {
                    append(",")
                }
            }
        }

        HighlightableAnnotatedText(
            annotatedText = valueText,
            searchQuery = searchQuery,
            highlightColor = highlightColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
    }
}

