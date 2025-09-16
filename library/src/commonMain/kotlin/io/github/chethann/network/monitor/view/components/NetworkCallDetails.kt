package io.github.chethann.network.monitor.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.chethann.network.monitor.db.NetworkCallEntity
import io.github.chethann.network.monitor.utils.copyToClipboard
import io.github.chethann.network.monitor.utils.toCurlString
import io.github.chethann.network.monitor.view.CollapsibleJsonViewer
import io.github.chethann.network.monitor.view.theme.extendedColors
import kotlinx.coroutines.launch

// Data class for search results
data class SearchResult(
    val startIndex: Int,
    val endIndex: Int,
    val matchedText: String
)

@Composable
fun NetworkCallDetails(item: NetworkCallEntity, onBackPress: () -> Unit) {

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var currentSearchIndex by remember { mutableStateOf(0) }

    // Scroll state
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Layout coordinates for each section to enable precise scrolling
    var sectionOffsets by remember { mutableStateOf<Map<Int, Float>>(emptyMap()) }

    // Helper function to clean body content by removing Content-Type headers
    fun cleanBodyContent(bodyContent: String?): String {
        if (bodyContent.isNullOrEmpty()) return ""

        // Remove Content-Type lines and BODY Content-Type lines from the body content
        return bodyContent.lines()
            .filterNot { line ->
                val trimmedLine = line.trim()
                trimmedLine.startsWith("Content-Type:", ignoreCase = true) ||
                trimmedLine.startsWith("BODY Content-Type:", ignoreCase = true)
            }
            .joinToString("\n")
            .trim()
    }
    // Themed semantic colors
    val requestColor = MaterialTheme.extendedColors.success
    val responseColor = MaterialTheme.colors.secondary
    val urlColor = MaterialTheme.colors.primaryVariant
    val methodColor = MaterialTheme.colors.primary
    val summaryColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)

    // Status code colors based on response type
    val statusColor = when {
        item.status == null -> MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        item.status in 200..299 -> MaterialTheme.extendedColors.success
        item.status in 300..399 -> MaterialTheme.extendedColors.info
        item.status in 400..499 -> MaterialTheme.extendedColors.warning
        item.status!! >= 500 -> MaterialTheme.colors.error
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
    }

    // Create searchable sections with their content
    val searchableSections = remember(item) {
        listOf(
            "URL" to item.fullUrl,
            "HTTP Method" to item.httpMethod,
            "Request Headers" to (item.requestHeaders ?: ""),
            "Request Body" to (item.requestBody ?: ""),
            "Response Summary" to (item.responseSummary ?: ""),
            "Response Code" to (item.status?.toString() ?: ""),
            "Response Headers" to (item.responseHeaders ?: ""),
            "Response Body" to (item.responseBody ?: "")
        )
    }

    // Simplified search functionality that tracks which section contains matches
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            val results = mutableListOf<SearchResult>()
            var totalMatches = 0

            searchableSections.forEachIndexed { sectionIndex, (sectionName, content) ->
                // Count occurrences in this section
                var occurrenceCount = 0
                var startIndex = content.indexOf(searchQuery, ignoreCase = true)
                while (startIndex != -1) {
                    occurrenceCount++
                    startIndex = content.indexOf(searchQuery, startIndex + 1, ignoreCase = true)
                }

                if (occurrenceCount > 0) {
                    totalMatches += occurrenceCount
                    results.add(SearchResult(
                        startIndex = sectionIndex,
                        endIndex = totalMatches, // Store total count in endIndex for display
                        matchedText = "$sectionName ($occurrenceCount ${if (occurrenceCount == 1) "match" else "matches"})"
                    ))
                }
            }
            searchResults = results
            currentSearchIndex = 0
        } else {
            searchResults = emptyList()
        }
    }

    // Scroll to the section containing the current search result
    fun scrollToCurrentResult() {
        if (searchResults.isNotEmpty() && currentSearchIndex >= 0 && currentSearchIndex < searchResults.size) {
            coroutineScope.launch {
                val sectionIndex = searchResults[currentSearchIndex].startIndex
                val targetOffset = sectionOffsets[sectionIndex]
                if (targetOffset != null) {
                    // Scroll to the section with some padding from the top
                    val scrollTarget = kotlin.math.max(0f, targetOffset - 100f)
                    val maxScroll = kotlin.math.max(0, scrollState.maxValue)
                    val finalTarget = kotlin.math.min(scrollTarget.toInt(), maxScroll)
                    scrollState.animateScrollTo(finalTarget)
                }
            }
        }
    }

    // Auto-scroll when search result changes
    LaunchedEffect(currentSearchIndex, searchResults, sectionOffsets) {
        scrollToCurrentResult()
    }

    // Helper function to determine highlight color based on focus
    val focusedHighlightColor = MaterialTheme.extendedColors.focusHighlight
    val regularHighlightColor = MaterialTheme.extendedColors.highlight

    fun getHighlightColorForSection(sectionIndex: Int): Color {
        if (searchResults.isEmpty() || currentSearchIndex < 0 || currentSearchIndex >= searchResults.size) {
            return regularHighlightColor
        }
        val currentResultSectionIndex = searchResults[currentSearchIndex].startIndex
        return if (sectionIndex == currentResultSectionIndex) focusedHighlightColor else regularHighlightColor
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)
        .statusBarsPadding()
        .navigationBarsPadding()) {

        // Top bar with action buttons and search - Fixed at top
        Column(
            modifier = Modifier
                .fillMaxWidth()
        .background(MaterialTheme.colors.surface)
                .padding(16.dp)
        ) {
            // Action buttons row
            Row {
                Button(
                    onClick = onBackPress,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primaryVariant)
                ) {
                    Text("← Back", color = MaterialTheme.colors.onPrimary)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { copyToClipboard(item.toCurlString()) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.extendedColors.success)
                ) {
                    Text("📋 Copy Curl", color = MaterialTheme.colors.onPrimary)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        if (isSearchVisible) {
                            isSearchVisible = false
                            searchQuery = ""
                        } else {
                            isSearchVisible = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isSearchVisible) MaterialTheme.extendedColors.warning else MaterialTheme.colors.primary
                    )
                ) {
                    Text(if (isSearchVisible) "❌ Close" else "🔍 Search", color = MaterialTheme.colors.onPrimary)
                }
            }

            // Search bar (only visible when search is active)
            if (isSearchVisible) {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search in details...") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                leadingIcon = {
                                    Text("🔍", fontSize = 16.sp)
                                },
                                trailingIcon = if (searchQuery.isNotEmpty()) {
                                    {
                                        IconButton(
                                            onClick = { searchQuery = "" }
                                        ) {
                                            Text("❌", fontSize = 12.sp)
                                        }
                                    }
                                } else null
                            )
                        }

                        // Search results navigation
                        if (searchResults.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Calculate total matches from the last search result's endIndex
                            val totalMatches = searchResults.lastOrNull()?.endIndex ?: 0

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Found $totalMatches total ${if (totalMatches == 1) "match" else "matches"} in ${searchResults.size} ${if (searchResults.size == 1) "section" else "sections"}",
                                    color = Color(0xFF666666),
                                    fontSize = 12.sp
                                )

                                if (searchResults.size > 1) {
                                    Row {
                                        Text(
                                            "${currentSearchIndex + 1}/${searchResults.size}",
                                            color = Color(0xFF666666),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )

                                        Button(
                                            onClick = {
                                                currentSearchIndex = if (currentSearchIndex > 0) {
                                                    currentSearchIndex - 1
                                                } else {
                                                    searchResults.size - 1
                                                }
                                                scrollToCurrentResult()
                                            },
                                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF757575)),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("↑", color = Color.White, fontSize = 12.sp)
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Button(
                                            onClick = {
                                                currentSearchIndex = if (currentSearchIndex < searchResults.size - 1) {
                                                    currentSearchIndex + 1
                                                } else {
                                                    0
                                                }
                                                scrollToCurrentResult()
                                            },
                                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF757575)),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("↓", color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            // Show details of current section
                            if (currentSearchIndex < searchResults.size) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Current: ${searchResults[currentSearchIndex].matchedText}",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No results found",
                                    color = MaterialTheme.colors.error,
                                    fontSize = 12.sp
                                )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Start typing to search through network call details...",
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                        }
                    }
                }
            }
        }

        // Scrollable content with layout tracking
        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)) {

            // URL Section
            Column(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    sectionOffsets = sectionOffsets + (0 to coordinates.positionInParent().y)
                }
            ) {
                Text("🌐 URL", fontWeight = FontWeight.Bold, color = urlColor, fontSize = 16.sp)
                SelectionContainer {
                    HighlightableText(
                        text = item.fullUrl,
                        searchQuery = searchQuery,
                        highlightColor = regularHighlightColor,
                        textColor = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(8.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp)).padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // HTTP Method Section
            Column(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    sectionOffsets = sectionOffsets + (1 to coordinates.positionInParent().y)
                }
            ) {
                Text("📡 HTTP Method", fontWeight = FontWeight.Bold, color = methodColor, fontSize = 16.sp)
                SelectionContainer {
                    HighlightableText(
                        text = item.httpMethod,
                        searchQuery = searchQuery,
                        highlightColor = regularHighlightColor,
                        textColor = MaterialTheme.colors.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                            .background(methodColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Request Headers Section
            Column(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    sectionOffsets = sectionOffsets + (2 to coordinates.positionInParent().y)
                }
            ) {
                Text("📥 Request Headers", fontWeight = FontWeight.Bold, color = requestColor, fontSize = 16.sp)
                SelectionContainer {
                    HighlightableText(
                        text = "${item.requestHeaders}",
                        searchQuery = searchQuery,
                        highlightColor = regularHighlightColor,
                        textColor = requestColor.darken(0.2f),
                        modifier = Modifier.padding(8.dp)
                            .background(requestColor.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Request Body Section
            Column(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    sectionOffsets = sectionOffsets + (3 to coordinates.positionInParent().y)
                }
            ) {
                // Add copy button for Request Body
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📤 Request Body", fontWeight = FontWeight.Bold, color = requestColor, fontSize = 16.sp)

                    if (!item.requestBody.isNullOrEmpty()) {
                        Button(
                            onClick = {
                                copyToClipboard(item.requestBody)
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = requestColor),
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("📋 Copy", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }

                SelectionContainer {
                    CollapsibleJsonViewer(
                        jsonContent = item.requestBody ?: "",
                        searchQuery = searchQuery,
                        highlightColor = regularHighlightColor,
                        modifier = Modifier.padding(8.dp)
                            .background(requestColor.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                            .padding(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Response Summary Section
            Column(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    sectionOffsets = sectionOffsets + (4 to coordinates.positionInParent().y)
                }
            ) {
                Text("📊 Response Summary", fontWeight = FontWeight.Bold, color = summaryColor, fontSize = 16.sp)
                HighlightableText(
                    text = if (item.responseSummary.isNullOrEmpty()) "No summary" else item.responseSummary,
                    searchQuery = searchQuery,
                    highlightColor = regularHighlightColor,
                    textColor = if (item.responseSummary.isNullOrEmpty()) MaterialTheme.colors.onSurface.copy(alpha = 0.4f) else MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(8.dp)
                        .background(MaterialTheme.colors.onSurface.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Response Code Section
            Column(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    sectionOffsets = sectionOffsets + (5 to coordinates.positionInParent().y)
                }
            ) {
                Text("🎯 Response Code", fontWeight = FontWeight.Bold, color = responseColor, fontSize = 16.sp)
                HighlightableText(
                    text = "${item.status ?: "Unknown"}",
                    searchQuery = searchQuery,
                    highlightColor = regularHighlightColor,
                    textColor = MaterialTheme.colors.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(8.dp)
                        .background(statusColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Response Headers Section
            Column(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    sectionOffsets = sectionOffsets + (6 to coordinates.positionInParent().y)
                }
            ) {
                Text("📩 Response Headers", fontWeight = FontWeight.Bold, color = responseColor, fontSize = 16.sp)
                SelectionContainer {
                    HighlightableText(
                        text = "${item.responseHeaders}",
                        searchQuery = searchQuery,
                        highlightColor = regularHighlightColor,
                        textColor = responseColor.darken(0.2f),
                        modifier = Modifier.padding(8.dp)
                            .background(responseColor.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Response Body Section
            Column(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    sectionOffsets = sectionOffsets + (7 to coordinates.positionInParent().y)
                }
            ) {
                // Add copy button for Response Body
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📄 Response Body", fontWeight = FontWeight.Bold, color = responseColor, fontSize = 16.sp)

                    if (!item.responseBody.isNullOrEmpty()) {
                        Button(
                            onClick = {
                                copyToClipboard(cleanBodyContent(item.responseBody))
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = responseColor),
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("📋 Copy", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }

                SelectionContainer {
                    CollapsibleJsonViewer(
                        jsonContent = item.responseBody ?: "",
                        searchQuery = searchQuery,
                        highlightColor = regularHighlightColor,
                        modifier = Modifier.padding(8.dp)
                            .background(responseColor.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

// Simple utility to darken a color by a given factor (0f..1f)
private fun Color.darken(factor: Float): Color {
    val f = 1 - factor.coerceIn(0f, 1f)
    return Color(
        red = (red * f),
        green = (green * f),
        blue = (blue * f),
        alpha = alpha
    )
}
