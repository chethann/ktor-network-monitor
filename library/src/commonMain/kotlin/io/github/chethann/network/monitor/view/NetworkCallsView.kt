package io.github.chethann.network.monitor.view

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    // Load network calls on composition
    LaunchedEffect(Unit) {
        loadNetworkCalls { calls ->
            networkCalls = calls
        }
    }

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
        onSearchClick = { query ->
            // Search functionality is now handled within NetworkCallsList component
        },
        onRefreshClick = {
            CoroutineScope(Dispatchers.IO).launch {
                isRefreshing = true
                loadNetworkCalls { calls ->
                    networkCalls = calls
                    isRefreshing = false
                }
            }
        }
    )
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
