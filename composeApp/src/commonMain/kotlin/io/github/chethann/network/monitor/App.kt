package io.github.chethann.network.monitor

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.chethann.network.monitor.network.NetworkClient
import io.github.chethann.network.monitor.network.TestPostBody
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.parameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        SampleApp()
    }
}

@Composable
fun SampleApp() {
    val ioScope = remember { CoroutineScope(Dispatchers.IO) }
    val networkClient = remember {  NetworkClient.getHttpClient() }
    Column {
        Button(onClick = {
            ioScope.launch {
                networkClient.get("${getLocalHost()}/successEndpoint") {
                    parameter("param1", "param1")
                }
            }
        }) {
            Text("Make Get request")
        }

        Button(onClick = {
            ioScope.launch {
                networkClient.post("${getLocalHost()}/postEndpoint") {
                    setBody(
                        TestPostBody(
                            id = "SomeId"
                        )
                    )
                    contentType(ContentType.Application.Json)
                }
            }
        }) {
            Text("Make Post request")
        }

        Button(onClick = {
            ioScope.launch {
                networkClient.get("${getLocalHost()}/errorEndpoint")
            }
        }) {
            Text("Make Get request - Error")
        }

    }
}

expect fun getLocalHost(): String