package io.github.chethann.network.monitor.network

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*

internal class LoggedContent(
    private val originalContent: OutgoingContent,
    private val channel: ByteReadChannel
) : OutgoingContent.ReadChannelContent() {

    override val contentType: ContentType? = originalContent.contentType
    override val contentLength: Long? = originalContent.contentLength
    override val status: HttpStatusCode? = originalContent.status
    override val headers: Headers = originalContent.headers

    override fun <T : Any> getProperty(key: AttributeKey<T>): T? = originalContent.getProperty(key)

    override fun <T : Any> setProperty(key: AttributeKey<T>, value: T?) =
        originalContent.setProperty(key, value)

    override fun readFrom(): ByteReadChannel = channel
}
