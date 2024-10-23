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
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.statement.HttpReceivePipeline
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpResponsePipeline
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.util.AttributeKey
import io.ktor.util.InternalAPI
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
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

private val UUID_KEY = AttributeKey<String>("UUID_KEY")

@OptIn(ExperimentalUuidApi::class)
class NetworkCallsMonitor private constructor() {

    class Config {

    }

    companion object Plugin : HttpClientPlugin<Config, NetworkCallsMonitor> {

        private val database = DBInstanceProvider.getNetworkMonitorDB()

        override val key: AttributeKey<NetworkCallsMonitor> = AttributeKey("NetworkCallsMonitor")

        override fun prepare(block: Config.() -> Unit): NetworkCallsMonitor {
            val config = Config().apply(block)
            return NetworkCallsMonitor()
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
        val content = request.body as OutgoingContent

        val message = buildString {
            appendLine("REQUEST: ${Url(request.url)}")
            appendLine("METHOD: ${request.method}")

            appendLine("COMMON HEADERS")
            logHeaders(request.headers.entries())

            appendLine("CONTENT HEADERS")
            content.contentLength?.let {
                logHeader(HttpHeaders.ContentLength, it.toString())
            }
            content.contentType?.let {
                logHeader(HttpHeaders.ContentType, it.toString())
            }
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
                    httpRequestType = request.method.value,
                    requestTimestamp = getCurrentTime()
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
        val requestLog = StringBuilder()
        requestLog.appendLine("BODY Content-Type: ${content.contentType}")

        val charset = content.contentType?.charset() ?: Charsets.UTF_8

        val channel = ByteChannel()
        GlobalScope.launch(Dispatchers.Unconfined) {
            val text = channel.tryReadText(charset) ?: "[request body omitted]"
            requestLog.appendLine("BODY START")
            requestLog.appendLine(text)
            requestLog.append("BODY END")
        }.invokeOnCompletion {
            logToDB {
                database.getNetworkCallDao().updateNetworkCall(NetworkRequestBody(
                    id = uuid,
                    requestBody = requestLog.toString()
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


    @OptIn(InternalAPI::class)
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
                logResponseBody(log, it.contentType(), it.content, uuid, responseCode)
            } catch (cause: Throwable) {
                logResponseException(cause.message ?: "Failed", uuid)
            }
        }

        ResponseObserver.install(ResponseObserver(observer), client)
    }

    private fun logResponseHeader(
        log: StringBuilder,
        response: HttpResponse,
        uuid: String
    ) {
        with(log) {
            appendLine("RESPONSE: ${response.status}")
            appendLine("METHOD: ${response.call.request.method}")
            appendLine("FROM: ${response.call.request.url}")

            appendLine("COMMON HEADERS")
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
        content: ByteReadChannel,
        uuid: String,
        responseCode: Int
    ) {
        with(log) {
            appendLine("BODY Content-Type: $contentType")
            appendLine("BODY START")

            val message = content.tryReadText(contentType?.charset() ?: Charsets.UTF_8) ?: "[response body omitted]"
            appendLine(message)
            append("BODY END")

            logToDB {
                database.getNetworkCallDao().updateNetworkCall(NetworkResponseBody(
                    id = uuid,
                    responseBody = log.toString(),
                    responseSize = getFormattedResponseSize(content.totalBytesRead),
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

    private fun calculateResponseSize(byteArray: ByteArray): String {
        val sizeInBytes = byteArray.size // size in bytes
        return getFormattedResponseSize(sizeInBytes * 1L)
    }

    private fun getFormattedResponseSize(sizeInBytes: Long): String {
        val sizeInKB = sizeInBytes / 1024.0 // size in kilobytes (1 KB = 1024 bytes)

        if (sizeInBytes < 1024.0) {
            return "$sizeInBytes Bytes"
        }
        return "$sizeInKB KB"
    }
}