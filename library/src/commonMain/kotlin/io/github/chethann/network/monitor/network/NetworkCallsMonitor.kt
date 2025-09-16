@file:OptIn(InternalAPI::class)

package io.github.chethann.network.monitor.network

import io.github.chethann.network.monitor.db.DBInstanceProvider
import io.github.chethann.network.monitor.db.NetworkCallEntity
import io.github.chethann.network.monitor.db.NetworkRequestBody
import io.github.chethann.network.monitor.db.NetworkResponseBody
import io.github.chethann.network.monitor.db.NetworkResponseHeaders
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.observer.ResponseHandler
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.statement.DefaultHttpResponse
import io.ktor.client.statement.HttpReceivePipeline
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpResponsePipeline
import io.ktor.client.statement.content
import io.ktor.http.ContentType
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.util.AttributeKey
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val UUID_KEY = AttributeKey<String>("NETWORK_UUID_KEY")

@OptIn(ExperimentalUuidApi::class)
class NetworkCallsMonitor private constructor(
    private val filteredEndpoints: List<String> = emptyList()
) {

    class Config {
        /**
         * List of endpoint patterns to filter out from monitoring.
         * Supports exact matches and wildcard patterns with '*'.
         * Examples:
         * - "/api/health" (exact match)
         * - "/api/internal/*" (wildcard pattern)
         * - "*/metrics" (wildcard pattern)
         *
         * Default: empty list (no filtering)
         */
        var filteredEndpoints: List<String> = emptyList()
            set(value) {
                field = value
            }
    }

    companion object Plugin : HttpClientPlugin<Config, NetworkCallsMonitor> {

        private val database = DBInstanceProvider.getNetworkMonitorDB()

        override val key: AttributeKey<NetworkCallsMonitor> = AttributeKey("NetworkCallsMonitor")

        override fun prepare(block: Config.() -> Unit): NetworkCallsMonitor {
            val config = Config().apply(block)
            return NetworkCallsMonitor(config.filteredEndpoints)
        }

        override fun install(plugin: NetworkCallsMonitor, scope: HttpClient) {
            plugin.setupRequestLogging(scope)
            plugin.setupResponseLogging(scope)
        }
    }

    private fun setupRequestLogging(client: HttpClient) {
        client.sendPipeline.intercept(HttpSendPipeline.Monitoring) {

            val response = try {
                logRequest(context)
            } catch (_: Throwable) {
                null
            }

            try {
                proceedWith(response?.first ?: subject)
            } catch (cause: Throwable) {
                if (response?.second != null) {
                    logRequestException(cause, response.second)
                }
                throw cause
            }
        }
    }

    private suspend fun logRequest(request: HttpRequestBuilder): Pair<OutgoingContent?, String> {
        // Check if this endpoint should be filtered out
        val requestPath = request.url.encodedPath
        val fullUrl = request.url.toString()

        if (shouldFilterEndpoint(requestPath, fullUrl)) {
            // Return early without logging if endpoint is filtered
            return Pair(null, "")
        }

        val content = request.body as OutgoingContent

        val message = buildString {
            logHeaders(request.headers.entries())
            // todo: See if we should save this info in other fields
            /*appendLine("CONTENT HEADERS")
            content.contentLength?.let {
                logHeader(HttpHeaders.ContentLength, it.toString())
            }
            content.contentType?.let {
                logHeader(HttpHeaders.ContentType, it.toString())
            }*/
            logHeaders(content.headers.entries())
        }

        val uuid = Uuid.random().toString()
        request.attributes.put(UUID_KEY, uuid)

        if (message.isNotEmpty()) {
            logToDB {
                database.getNetworkCallDao().addNetworkCall(NetworkCallEntity(
                    id = uuid,
                    relativeUrl = request.url.encodedPath,
                    host = request.url.host,
                    requestHeaders = message,
                    httpRequestType = request.method.value,
                    requestTimestamp = getCurrentTime(),
                    httpMethod = request.method.value,
                    fullUrl = request.url.toString()
                ))
            }
        }

        if (message.isEmpty()) {
            return Pair(null, uuid)
        }

        return Pair(logRequestBody(content, uuid), uuid)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun logRequestBody(
        content: OutgoingContent,
        uuid: String
    ): OutgoingContent {
        val charset = content.contentType?.charset() ?: Charsets.UTF_8
        val channel = ByteChannel()
        GlobalScope.launch(Dispatchers.Unconfined) {
        }.invokeOnCompletion {
            logToDB {
                database.getNetworkCallDao().updateNetworkCall(NetworkRequestBody(
                    id = uuid,
                    requestBody = channel.tryReadText(charset) ?: "[request body omitted]",
                    requestContentType = content.contentType?.contentType
                ))
            }
        }

        return content.observe(channel)
    }

    private suspend fun logRequestException(cause: Throwable, uuid: String) {
        logToDB {
            database.getNetworkCallDao().updateNetworkCall(NetworkResponseBody(
                id = uuid,
                isSuccess = false,
                responseSummary = "Network call failed due to \n ${cause.message}"
            ))
        }
    }


    private fun setupResponseLogging(client: HttpClient) {
        client.receivePipeline.intercept(HttpReceivePipeline.State) { response ->
            val uuid = response.call.attributes.getOrNull(UUID_KEY) ?: return@intercept
            val header = StringBuilder()

            try {
                logResponseHeader(
                    header,
                    response.call.response,
                    uuid
                )
                proceedWith(subject)
            } catch (cause: Throwable) {
                logResponseException(header, response.call.request, cause, uuid)
                throw cause
            } finally {
                logResponseHeader(header.toString(), uuid)
            }
        }

        client.responsePipeline.intercept(HttpResponsePipeline.Receive) { response ->
            try {
                proceed()
            } catch (cause: Throwable) {
                // todo: Log response exception here, right now it's not possible to get a handle to uuid here
                //logResponseException(cause.message ?: "Failed", "uuid")
                throw cause
            }
        }

        val observer: ResponseHandler = observer@{
            val uuid = it.call.attributes.getOrNull(UUID_KEY) ?: return@observer
            val responseCode = it.call.response.status.value
            val log = StringBuilder()
            try {
                logResponseBody(log, it.contentType(), it, uuid, responseCode)
            } catch (cause: Throwable) {
                logResponseException(cause.message ?: "Failed", uuid)
            }
        }

        ResponseObserver.install(ResponseObserver.prepare { onResponse(observer); }, client)
    }

    private fun logResponseHeader(
        log: StringBuilder,
        response: HttpResponse,
        uuid: String
    ) {
        with(log) {
            logHeaders(response.headers.entries())

            logToDB {
                database.getNetworkCallDao().updateNetworkCall(NetworkResponseHeaders(
                    id = uuid,
                    responseHeaders = log.toString()
                ))
            }
        }
    }

    private suspend fun logResponseHeader(headersString: String, uuid: String) {
        logToDB {
            database.getNetworkCallDao().updateNetworkCall(NetworkResponseHeaders(
                id = uuid,
                responseHeaders = headersString
            ))
        }
    }

    private suspend fun logResponseBody(
        log: StringBuilder,
        contentType: ContentType?,
        response: HttpResponse,
        uuid: String,
        responseCode: Int
    ) {
        with(log) {
            appendLine("BODY Content-Type: $contentType")
            val responseText = response.content.tryReadText(contentType?.charset() ?: Charsets.UTF_8)
            appendLine(responseText ?: "[response body omitted]")
            val responseLength = responseText?.length ?: 0
            logToDB {
                database.getNetworkCallDao().updateNetworkCall(NetworkResponseBody(
                    id = uuid,
                    responseBody = log.toString(),
                    responseSize = getFormattedResponseSize(responseLength),
                    //responseSize = "",
                    responseTimestamp = getCurrentTime(),
                    status = responseCode,
                    isSuccess = responseCode in 200..299
                ))
            }
        }
    }


    private suspend fun logResponseBody(message: String, uuid: String) {
        logResponseException("There was an error is logging network request. Summary: \n $message", uuid)
    }

    private suspend fun logResponseException(log: StringBuilder, request: HttpRequest, cause: Throwable, uuid: String) {
        val responseFailureReason = "RESPONSE ${request.url} failed with exception: $cause"
        logResponseException(responseFailureReason, uuid)
    }

    private suspend fun logResponseException(message: String, uuid: String) {
        logToDB {
            database.getNetworkCallDao().updateNetworkCall(
                NetworkResponseBody(
                    id = uuid,
                    responseSummary = message,
                    isSuccess = false
                ))
        }
    }

    private fun getCurrentTime(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }

    private fun logToDB(block: suspend () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            block.invoke()
        }
    }

    private fun getFormattedResponseSize(sizeInBytes: Int): String {
        val sizeInKB = sizeInBytes / 1024.0 // size in kilobytes (1 KB = 1024 bytes)

        if (sizeInBytes < 1024.0) {
            return "$sizeInBytes Bytes"
        }
        return "$sizeInKB KB"
    }

    /**
     * Checks if an endpoint should be filtered out based on the configured filter patterns.
     * Supports exact matches and wildcard patterns with '*'.
     */
    private fun shouldFilterEndpoint(requestPath: String, fullUrl: String): Boolean {
        return filteredEndpoints.any { pattern ->
            matchesPattern(pattern, requestPath) || matchesPattern(pattern, fullUrl)
        }
    }

    /**
     * Matches a URL path or full URL against a pattern that may contain wildcards (*).
     *
     *             "/api/health",           // Exact match
     *             "/api/internal/*",       // Filter all internal APIs
     *             "*/metrics",             // Filter metrics endpoints
     *             "https://example.com/ping" // Filter specific full URLs
     */
    private fun matchesPattern(pattern: String, url: String): Boolean {
        if (pattern == url) return true // Exact match

        if (!pattern.contains("*")) return false // No wildcard, already checked exact match

        // Convert pattern to regex by escaping special regex chars and replacing * with .*
        val regexPattern = pattern
            .replace("\\", "\\\\")
            .replace(".", "\\.")
            .replace("+", "\\+")
            .replace("?", "\\?")
            .replace("^", "\\^")
            .replace("$", "\\$")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("|", "\\|")
            .replace("*", ".*")

        return Regex("^$regexPattern$").matches(url)
    }

    // This function returns a copy of original response as reading of response is allowed only once
    private fun getDefaultHttpResponse(httpResponse: HttpResponse, byteReadChannel: ByteReadChannel): DefaultHttpResponse {
        return DefaultHttpResponse(
            call = httpResponse.call,
            HttpResponseData(
                requestTime = httpResponse.requestTime,
                statusCode = httpResponse.status,
                version = httpResponse.version,
                headers = httpResponse.headers,
                body = byteReadChannel,
                callContext = httpResponse.coroutineContext)
        )
    }
}