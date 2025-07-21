package io.github.chethann.network.monitor.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
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
import androidx.compose.ui.Alignment
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
import io.github.chethann.network.monitor.utils.copyToClipboard
import io.github.chethann.network.monitor.utils.toCurlString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val inProgressColor = Color(0xFFF0AD4E) // Light Amber
val successColor = Color(0xFF5CB85C)    // Light Green
val errorColor = Color(0xFFD9534F)      // Light Red

@Composable
fun NetworkCallsView() {
    var requests by remember { mutableStateOf(listOf<NetworkCallEntity>()) }

    val listState = rememberLazyListState()

    val dbInstance = remember {  DBInstanceProvider.getNetworkMonitorDB() }
    val ioDispatcher = remember { CoroutineScope(Dispatchers.IO) }
    val isRefreshing = remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing.value) {
        if (isRefreshing.value) {
            delay(1000)
            isRefreshing.value = false
        }
    }

    LaunchedEffect(Unit) {
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

    if (isRefreshing.value) {
        Text("refreshing")
        return
    }

    NetworkCallsListFullView(
        requests,
        lazyListState = listState,
        onClearClick = {
            ioDispatcher.launch { dbInstance.getNetworkCallDao().clearData() }
        },
        onRefreshClick = {
            isRefreshing.value = true
        },
        onSearchClick = {

        }
    )

}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NetworkCallsListFullView(networkCalls: List<NetworkCallEntity>, lazyListState: LazyListState, onClearClick: () -> Unit, onSearchClick: (String) -> Unit, onRefreshClick: () -> Unit) {

    currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    val navigator = rememberListDetailPaneScaffoldNavigator<NetworkCallEntity>()

    UnifiedBackHandler {
        if (navigator.canNavigateBack()) {
            CoroutineScope(Dispatchers.IO).launch {
                navigator.navigateBack()
            }
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
                        CoroutineScope(Dispatchers.IO).launch {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, item)
                        }
                    },
                    onClearClick = onClearClick,
                    onRefreshClick = onRefreshClick,
                    onSearchClick = onSearchClick
                )
            }
        },
        detailPane = {
            AnimatedPane {
                // Show the detail pane content if selected item is available
                navigator.currentDestination?.contentKey?.let {
                    NetworkCallDetails(it, onBackPress = {
                        if (navigator.canNavigateBack()) {
                            CoroutineScope(Dispatchers.IO).launch {
                                navigator.navigateBack()
                            }
                        }
                    })
                }
            }
        },
    )

}

@Composable
fun NetworkCallsList(networkCallEntities: List<NetworkCallEntity>, lazyListState: LazyListState, onItemClick: (NetworkCallEntity) -> Unit,
                     onClearClick: () -> Unit, onSearchClick: (String) -> Unit, onRefreshClick: () -> Unit) {

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.statusBarsPadding()
            .navigationBarsPadding()
    ) {
        item {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row {
                    Button(onClick = {
                        onRefreshClick()
                    }) {
                        Text("Refresh")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(onClick = {
                        onClearClick()
                    }) {
                        Text("Clear")
                    }
                }
            }
        }

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
fun NetworkCallDetails(item: NetworkCallEntity, onBackPress: () -> Unit) {

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.onPrimary)
        .statusBarsPadding()
        .navigationBarsPadding()
        .verticalScroll(
        rememberScrollState()
    )) {

        Button(
            onClick = onBackPress
        ) {
            Text("Back")
        }

        Button(
            onClick = {
                copyToClipboard(item.toCurlString())
            }
        ) {
            Text("Copy Curl")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Url:", fontWeight = FontWeight.Bold)
        SelectionContainer {
            Text(item.fullUrl)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text("HttpMethod:", fontWeight = FontWeight.Bold)
        SelectionContainer {
            Text(item.httpMethod)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text("Request headers", fontWeight = FontWeight.Bold)
        SelectionContainer {
            Text("${item.requestHeaders}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Request body", fontWeight = FontWeight.Bold)
        SelectionContainer {
            Text("${item.requestBody}")
        }
        Spacer(modifier = Modifier.height(16.dp))

        item.responseSummary?.let {
            Text("Request summary", fontWeight = FontWeight.Bold)
            Text(it)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("Response code", fontWeight = FontWeight.Bold)
        Text("${item.status}")

        Spacer(modifier = Modifier.height(16.dp))

        Text("Response headers", fontWeight = FontWeight.Bold)
        SelectionContainer {
            Text("${item.responseHeaders}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Response Body", fontWeight = FontWeight.Bold)
        SelectionContainer {
            Text("${item.responseBody}")
        }

    }
}
