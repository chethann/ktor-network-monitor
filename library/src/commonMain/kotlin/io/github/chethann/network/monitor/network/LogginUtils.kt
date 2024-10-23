package io.github.chethann.network.monitor.network

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.core.readText

internal fun Appendable.logHeaders(
    headers: Set<Map.Entry<String, List<String>>>,
) {
    val sortedHeaders = headers.toList().sortedBy { it.key }

    sortedHeaders.forEach { (key, values) ->
        logHeader(key, values.joinToString("; "))
    }
}

internal fun Appendable.logHeader(key: String, value: String) {
    appendLine("-> $key: $value")
}

internal suspend inline fun ByteReadChannel.tryReadText(charset: Charset): String? = try {
    readRemaining().readText(charset = charset)
} catch (cause: Throwable) {
    null
}