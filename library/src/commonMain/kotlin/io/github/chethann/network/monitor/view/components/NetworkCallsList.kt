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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.chethann.network.monitor.db.NetworkCallEntity
import kotlinx.coroutines.launch

@Composable
fun NetworkCallsList(
    networkCallEntities: List<NetworkCallEntity>,
    lazyListState: LazyListState,
    onItemClick: (NetworkCallEntity) -> Unit,
    onClearClick: () -> Unit,
    onSearchClick: (String) -> Unit,
    onRefreshClick: () -> Unit,
    isRefreshing: Boolean = false
) {
    // Search state - modified to make search sticky
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var filteredItems by remember { mutableStateOf(networkCallEntities) }
    var searchResults by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentSearchIndex by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Filter management state
    var isFilterManagementVisible by remember { mutableStateOf(false) }
    var filteredEndpoints by remember { mutableStateOf(setOf<String>()) }
    var showFilteredAPIs by remember { mutableStateOf(false) }

    fun getUniqueEndpoints(): Set<String> {
        return networkCallEntities.map { it.relativeUrl }.toSet()
    }

    // Filter items based on search query and endpoint filters
    LaunchedEffect(networkCallEntities, searchQuery, filteredEndpoints, showFilteredAPIs) {
        var itemsToProcess = networkCallEntities

        // Apply endpoint filtering first (if not showing filtered APIs)
        if (!showFilteredAPIs && filteredEndpoints.isNotEmpty()) {
            itemsToProcess = networkCallEntities.filter { item ->
                !shouldFilterEndpoint(item.relativeUrl, item.fullUrl, filteredEndpoints)
            }
        }

        if (searchQuery.isBlank()) {
            filteredItems = itemsToProcess
            searchResults = emptyList()
        } else {
            val results = mutableListOf<Int>()
            val filtered = itemsToProcess.filterIndexed { index, item ->
                val matches = item.fullUrl.contains(searchQuery, ignoreCase = true) ||
                        item.relativeUrl.contains(searchQuery, ignoreCase = true) ||
                        item.host.contains(searchQuery, ignoreCase = true) ||
                        item.httpMethod.contains(searchQuery, ignoreCase = true) ||
                        item.status?.toString()?.contains(searchQuery, ignoreCase = true) == true

                if (matches) {
                    results.add(index)
                }
                matches
            }
            filteredItems = filtered
            searchResults = results
            currentSearchIndex = 0
        }
    }

    // Reset search state when network calls list is cleared (becomes empty)
    LaunchedEffect(networkCallEntities.isEmpty()) {
        if (networkCallEntities.isEmpty()) {
            searchQuery = ""
            isSearchVisible = false
            filteredItems = emptyList()
            searchResults = emptyList()
            currentSearchIndex = 0
        }
    }

    // Auto-scroll to current search result
    LaunchedEffect(currentSearchIndex, searchResults) {
        if (searchResults.isNotEmpty() && currentSearchIndex >= 0 && currentSearchIndex < searchResults.size) {
            coroutineScope.launch {
                // No need to add +1 since header is now outside LazyColumn
                lazyListState.animateScrollToItem(currentSearchIndex)
            }
        }
    }

    // Helper to update search index and trigger scroll for NetworkCallsList
    fun updateSearchIndex(newIndex: Int) {
        currentSearchIndex = newIndex
        // Launch scroll after index update for list items
        coroutineScope.launch {
            if (filteredItems.isNotEmpty() && currentSearchIndex >= 0 && currentSearchIndex < filteredItems.size) {
                lazyListState.animateScrollToItem(currentSearchIndex)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(Color(0xFFF8F9FA))
    ) {
        // Fixed Header with Search - stays at top when scrolling
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Button(
                        onClick = { onRefreshClick() },
                        enabled = !isRefreshing,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isRefreshing) Color(0xFF9E9E9E) else Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isRefreshing) "⏳ Refreshing..." else "🔄 Refresh",
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onClearClick() },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFF44336)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🗑️ Clear", color = Color.White)
                    }
                }

                Row {
                    // Search toggle button
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
                            backgroundColor = if (isSearchVisible) Color(0xFFFF9800) else Color(0xFF2196F3)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isSearchVisible) "❌ Close" else "🔍 Search", color = Color.White)
                    }
                }
            }

            // Second row for Filter button - mobile friendly
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Filter management button - smaller and on separate row
                Button(
                    onClick = {
                        isFilterManagementVisible = !isFilterManagementVisible
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (filteredEndpoints.isNotEmpty()) Color(0xFF9C27B0) else Color(0xFF795548)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (filteredEndpoints.isNotEmpty()) "🚫 ${filteredEndpoints.size}" else "🚫 Filter",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }

            // Search bar (visible when search is active) - now sticky once opened
            if (isSearchVisible) {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search by URL, method, status, headers...") },
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Found ${filteredItems.size} results",
                                    color = Color(0xFF666666),
                                    fontSize = 12.sp
                                )

                                // Show navigation buttons even for single result
                                Row {
                                    Text(
                                        "${currentSearchIndex + 1}/${filteredItems.size}",
                                        color = Color(0xFF666666),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )

                                    if (filteredItems.size > 1) {
                                        Button(
                                            onClick = {
                                                if (currentSearchIndex > 0) {
                                                    updateSearchIndex(currentSearchIndex - 1)
                                                } else {
                                                    updateSearchIndex(filteredItems.size - 1)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = Color(0xFF607D8B)
                                            ),
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(4.dp)
                                        ) {
                                            Text("↑", color = Color.White, fontSize = 10.sp)
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    Button(
                                        onClick = {
                                            if (filteredItems.size > 1) {
                                                if (currentSearchIndex < filteredItems.size - 1) {
                                                    updateSearchIndex(currentSearchIndex + 1)
                                                } else {
                                                    updateSearchIndex(0)
                                                }
                                            } else {
                                                // For single result, just scroll to it
                                                updateSearchIndex(0)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = Color(0xFF607D8B)
                                        ),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("↓", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                            }
                        } else if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No results found",
                                color = Color(0xFFFF5722),
                                fontSize = 12.sp
                            )
                        } else {
                            // Show helpful text when search is open but empty
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Start typing to search through network calls...",
                                color = Color(0xFF666666),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Filter Management UI (visible when filter management is active)
            if (isFilterManagementVisible) {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Filter Management",
                                color = Color(0xFF333333),
                                fontSize = 14.sp
                            )

                            // Toggle to show/hide filtered APIs
                            Button(
                                onClick = { showFilteredAPIs = !showFilteredAPIs },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (showFilteredAPIs) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                                ),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(8.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    if (showFilteredAPIs) "👁️ Show All" else "🙈 Hide Filtered",
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Available endpoints to filter
                        val uniqueEndpoints = getUniqueEndpoints()
                        if (uniqueEndpoints.isNotEmpty()) {
                            Text(
                                "Available Endpoints (tap to filter):",
                                color = Color(0xFF666666),
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Display endpoints in a wrapped layout
                            val chunkedEndpoints = uniqueEndpoints.chunked(2)
                            chunkedEndpoints.forEach { endpointPair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    endpointPair.forEach { endpoint ->
                                        val isFiltered = filteredEndpoints.contains(endpoint)
                                        Button(
                                            onClick = {
                                                filteredEndpoints = if (isFiltered) {
                                                    filteredEndpoints - endpoint
                                                } else {
                                                    filteredEndpoints + endpoint
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = if (isFiltered) Color(0xFFF44336) else Color(0xFF2196F3)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp),
                                            contentPadding = PaddingValues(4.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (isFiltered) "❌ $endpoint" else "➕ $endpoint",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    // Add spacer if odd number of endpoints in the last row
                                    if (endpointPair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // Clear all filters button
                            if (filteredEndpoints.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { filteredEndpoints = emptySet() },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFFFF9800)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "🗑️ Clear All Filters",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            Text(
                                "No endpoints available to filter",
                                color = Color(0xFF999999),
                                fontSize = 12.sp
                            )
                        }

                        // Filter status summary
                        if (filteredEndpoints.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val filteredCount = networkCallEntities.count { item ->
                                shouldFilterEndpoint(item.relativeUrl, item.fullUrl, filteredEndpoints)
                            }
                            Text(
                                "Filtering ${filteredEndpoints.size} endpoint(s), hiding $filteredCount API call(s)",
                                color = Color(0xFF666666),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Stats summary
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val totalRequests = filteredItems.size
                val successCount = filteredItems.count { it.isSuccess && !it.inProgress }
                val errorCount = filteredItems.count { !it.isSuccess && !it.inProgress }
                val inProgressCount = filteredItems.count { it.inProgress }

                StatsChip("📊 Total", totalRequests.toString(), Color(0xFF9E9E9E))
                StatsChip("✅ Success", successCount.toString(), Color(0xFF4CAF50))
                StatsChip("❌ Error", errorCount.toString(), Color(0xFFF44336))
                StatsChip("🔄 Progress", inProgressCount.toString(), Color(0xFFFF9800))
            }
        }

        // Scrollable content (LazyColumn) - header is now outside and fixed
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
        ) {
            items(filteredItems, key = { it.id }) { item ->
                EnhancedNetworkCallListItem(
                    item = item,
                    onItemClick = onItemClick,
                    searchQuery = searchQuery,
                    isHighlighted = searchQuery.isNotEmpty() &&
                        filteredItems.indexOf(item) == currentSearchIndex
                )
            }
        }
    }
}

// Helper functions for filtering
private fun shouldFilterEndpoint(requestPath: String, fullUrl: String, patterns: Set<String>): Boolean {
    return patterns.any { pattern ->
        matchesPattern(pattern, requestPath) || matchesPattern(pattern, fullUrl)
    }
}

private fun matchesPattern(pattern: String, url: String): Boolean {
    if (pattern == url) return true
    if (!pattern.contains("*")) return false
    val regexPattern = pattern
        .replace("\\", "\\\\").replace(".", "\\.")
        .replace("+", "\\+").replace("?", "\\?")
        .replace("^", "\\^").replace("$", "\\$")
        .replace("(", "\\(").replace(")", "\\)")
        .replace("[", "\\[").replace("]", "\\]")
        .replace("{", "\\{").replace("}", "\\}")
        .replace("|", "\\|").replace("*", ".*")
    return Regex("^$regexPattern$").matches(url)
}
