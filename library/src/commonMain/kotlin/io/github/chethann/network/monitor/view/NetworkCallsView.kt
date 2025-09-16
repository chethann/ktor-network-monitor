package io.github.chethann.network.monitor.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.chethann.network.monitor.db.DBInstanceProvider
import io.github.chethann.network.monitor.db.NetworkCallEntity
import io.github.chethann.network.monitor.view.components.NetworkCallsListFullView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NetworkCallsView() {
    var networkCalls by remember { mutableStateOf<List<NetworkCallEntity>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    // Theme toggle state
    var darkTheme by remember { mutableStateOf(false) }
    var style by remember { mutableStateOf(io.github.chethann.network.monitor.view.theme.NetworkMonitorThemeStyle.Default) }
    var headerExpanded by remember { mutableStateOf(false) }

    val dbInstance = remember {  DBInstanceProvider.getNetworkMonitorDB() }
    val ioDispatcher = remember { CoroutineScope(Dispatchers.IO) }

    // Load network calls on composition
    LaunchedEffect(Unit) {
        dbInstance.getNetworkCallDao().getAllNetworkCallsFlow().collect {
            val didSizeChange = it.size != networkCalls.size
            val isScrolled = lazyListState.firstVisibleItemIndex != 0
            networkCalls = it
            if (!isScrolled && didSizeChange) { // New insertions should scroll the list and user has not scrolled the list, scroll to top
                delay(100)
                lazyListState.scrollToItem(0)
            }
        }
    }

    io.github.chethann.network.monitor.view.theme.NetworkMonitorTheme(
        darkTheme = darkTheme,
        style = style
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.systemBars)

        ) {
            // Top toggle bar
            if (headerExpanded) {
                Row(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { darkTheme = !darkTheme },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                    ) { Text(if (darkTheme) "🌙 Dark" else "🌞 Light", color = MaterialTheme.colors.onPrimary) }

                    Button(
                        onClick = {
                            style = when (style) {
                                io.github.chethann.network.monitor.view.theme.NetworkMonitorThemeStyle.Default -> io.github.chethann.network.monitor.view.theme.NetworkMonitorThemeStyle.Terminal
                                io.github.chethann.network.monitor.view.theme.NetworkMonitorThemeStyle.Terminal -> io.github.chethann.network.monitor.view.theme.NetworkMonitorThemeStyle.Default
                            }
                            if (style == io.github.chethann.network.monitor.view.theme.NetworkMonitorThemeStyle.Terminal) {
                                darkTheme = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                    ) { Text(if (style == io.github.chethann.network.monitor.view.theme.NetworkMonitorThemeStyle.Terminal) "</> Terminal" else "✨ Default", color = MaterialTheme.colors.onSecondary) }

                    Button(
                        onClick = { headerExpanded = false },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.15f))
                    ) { Text("▲ Hide", color = MaterialTheme.colors.onSurface) }
                }
            } else {
                Row(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.surface)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { headerExpanded = true },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primaryVariant)
                    ) { Text("▼ Show Controls", color = MaterialTheme.colors.onPrimary) }
                }
            }

            // Main content
            Box(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                NetworkCallsListFullView(
                    networkCalls = networkCalls,
                    lazyListState = lazyListState,
                    isRefreshing = isRefreshing,
                    onClearClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            DBInstanceProvider.getNetworkMonitorDB().getNetworkCallDao().clearData()
                            loadNetworkCalls { calls ->
                                networkCalls = calls
                            }
                        }
                    },
                    onSearchClick = { /* handled downstream */ },
                    onRefreshClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            isRefreshing = true
                            loadNetworkCalls { calls ->
                                networkCalls = calls
                                isRefreshing = false
                            }
                        }
                    },
                    showHeaderControls = headerExpanded
                )
            }
        }
    }
}

// Helper function to load network calls
private suspend fun loadNetworkCalls(onResult: (List<NetworkCallEntity>) -> Unit) {
    try {
        delay(1000) // Add 1 second delay to make refresh state visible to user
        val calls = DBInstanceProvider.getNetworkMonitorDB().getNetworkCallDao().getAllNetworkCalls()
        onResult(calls)
    } catch (e: Exception) {
        onResult(emptyList())
    }
}
