package io.github.chethann.network.monitor.network

import io.ktor.client.HttpClient

object NetworkClient {
    private lateinit var httpClient: HttpClient

    fun getHttpClient(): HttpClient {
        if (!::httpClient.isInitialized) {
            httpClient = createHttpClient()
        }
        return httpClient
    }
}

fun createHttpClient(): HttpClient {
    return HttpClient() {
        //install(NetworkCallsMonitorPlugin)
        //install(Logging)
        install(NetworkCallsMonitor)
        //install(NetworkCallsMonitorPluginTwo)
        //install(PipelineLoggingPlugin)
    }
}