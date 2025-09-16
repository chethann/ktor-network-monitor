package io.github.chethann.network.monitor.view.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.chethann.network.monitor.db.NetworkCallEntity
import io.github.chethann.network.monitor.view.UnifiedBackHandler

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NetworkCallsListFullView(
    networkCalls: List<NetworkCallEntity>,
    lazyListState: LazyListState,
    onClearClick: () -> Unit,
    onSearchClick: (String) -> Unit,
    onRefreshClick: () -> Unit,
    isRefreshing: Boolean = false,
    showHeaderControls: Boolean = true
) {
    val navController = rememberNavController()

    UnifiedBackHandler {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }

    NavHost(
        navController = navController,
        startDestination = "network_calls_list",
        modifier = Modifier.onKeyEvent {
            return@onKeyEvent true
        }
    ) {
        composable("network_calls_list") {
            NetworkCallsList(
                networkCallEntities = networkCalls,
                lazyListState = lazyListState,
                onItemClick = { item ->
                    // Navigate to the detail screen with the item ID
                    navController.navigate("network_call_detail/${item.id}")
                },
                onClearClick = onClearClick,
                onSearchClick = onSearchClick,
                onRefreshClick = onRefreshClick,
                isRefreshing = isRefreshing,
                showHeaderControls = showHeaderControls
            )
        }

        composable("network_call_detail/{callId}") { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId")
            val selectedCall = if (callId != null) {
                networkCalls.find { it.id == callId }
            } else {
                null
            }

            selectedCall?.let { call ->
                NetworkCallDetails(
                    item = call,
                    onBackPress = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
