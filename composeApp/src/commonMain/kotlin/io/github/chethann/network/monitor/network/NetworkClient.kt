package io.github.chethann.network.monitor.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

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
    return HttpClient {
        install(Logging)
        install(NetworkCallsMonitor)

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                isLenient = true
            })
        }
    }
}