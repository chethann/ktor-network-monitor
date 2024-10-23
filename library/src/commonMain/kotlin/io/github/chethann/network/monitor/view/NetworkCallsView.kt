package io.github.chethann.network.monitor.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.chethann.network.monitor.db.DBInstanceProvider
import io.github.chethann.network.monitor.db.NetworkCallEntity
import kotlinx.coroutines.delay

val inProgressColor = Color(0xFFF0AD4E) // Light Amber
val successColor = Color(0xFF5CB85C)    // Light Green
val errorColor = Color(0xFFD9534F)      // Light Red

@Composable
fun NetworkCallsView() {
    var requests by remember { mutableStateOf(listOf<NetworkCallEntity>()) }

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val dbInstance = DBInstanceProvider.getNetworkMonitorDB()
        dbInstance.getNetworkCallDao().getAllNetworkCallsFlow().collect {
            val didSizeChange = it.size != requests.size
            val isScrolled = listState.firstVisibleItemIndex != 0
            requests = it
            if (!isScrolled && didSizeChange) { // New insertions should scroll the list and user has not scrolled the list, scroll to top
                delay(100)
                listState.scrollToItem(0)
            }
        }
    }
    NetworkCallsList(requests, lazyListState = listState)

}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NetworkCallsList(networkCalls: List<NetworkCallEntity>, lazyListState: LazyListState) {

    currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    val navigator = rememberListDetailPaneScaffoldNavigator<NetworkCallEntity>()

    UnifiedBackHandler {
        if (navigator.canNavigateBack()) {
            navigator.navigateBack()
        }
    }


    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        modifier = Modifier.onKeyEvent {
            if (it.key == Key.A && it.type == KeyEventType.KeyDown) {
                println("A pressed")
            }
            return@onKeyEvent true
        },
        listPane = {
            AnimatedPane {
                NetworkCallsList(
                    networkCalls,
                    lazyListState,
                    onItemClick = { item ->
                        // Navigate to the detail pane with the passed item
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, item)
                    }
                )
            }
        },
        detailPane = {
            AnimatedPane {
                // Show the detail pane content if selected item is available
                navigator.currentDestination?.content?.let {
                    NetworkCallDetails(it)
                }
            }
        },
    )

}

@Composable
fun NetworkCallsList(networkCallEntities: List<NetworkCallEntity>, lazyListState: LazyListState, onItemClick: (NetworkCallEntity) -> Unit) {
    LazyColumn(
        state = lazyListState
    ) {
        items(networkCallEntities, key = { it.id }) {
            NetworkCallListItem(it, onItemClick)
        }
    }
}

@Composable
private fun NetworkCallListItem(item: NetworkCallEntity, onItemClick: (NetworkCallEntity) -> Unit) {

    val backgroundColor = if (item.inProgress) {
        inProgressColor
    } else {
        if (item.isSuccess) {
            successColor
        } else {
            errorColor
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable {
                onItemClick(item)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val responseCode: String = item.status?.toString() ?: ""

            Text(
                responseCode,
                modifier = Modifier.padding(end = 8.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(1f)
            ) {

                Text(
                    item.relativeUrl,
                    fontWeight = FontWeight.Bold
                )

                Text(item.host)

                Row {
                    Text(TimeDisplayUtils.getReadableTime(item.requestTimestamp), fontWeight = FontWeight.Thin)
                    item.responseSize?.let {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(item.responseSize, fontWeight = FontWeight.Thin)
                    }
                    if (item.responseTimestamp != 0L && item.requestTimestamp != 0L) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("${item.responseTimestamp - item.requestTimestamp} ms", fontWeight = FontWeight.Thin)
                    }
                }

            }
        }

        Divider(thickness = 1.dp)
    }
}

@Composable
fun NetworkCallDetails(item: NetworkCallEntity) {

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.onPrimary).verticalScroll(
        rememberScrollState()
    )) {

        item.responseSummary?.let {
            Text("Request summary: \n $it")
        }

        SelectionContainer {
            Text("Request headers: \n ${item.requestHeaders}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        SelectionContainer {
            Text("Request body: \n ${item.requestBody}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Response code: ${item.status}")

        Spacer(modifier = Modifier.height(16.dp))

        Text("Time taken code: ${item.responseTimestamp - item.requestTimestamp} ms")

        Spacer(modifier = Modifier.height(16.dp))

        Text("Response Size: ${item.responseSize}")

        Spacer(modifier = Modifier.height(16.dp))

        SelectionContainer {
            Text("Response headers: \n ${item.responseHeaders}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        SelectionContainer {
            Text("Response Body: \n ${item.responseBody}")
        }

    }
}

/*
@Composable
fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Use keyboard input handling for back navigation (Escape key)
    LaunchedEffect(enabled) {
        if (enabled) {
            Modifier.onKeyEvent {
                if (it.key == Key.A && it.type == KeyEventType.KeyDown) {
                    onBack()
                    true
                } else {
                    false
                }
            }
        }
    }
}*/
