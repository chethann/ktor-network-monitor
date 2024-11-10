package io.github.chethann.network.monitor.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable


@Entity(tableName = "networkCalls")
@Serializable
data class NetworkCallEntity(
    @PrimaryKey
    val id: String = "0",

    @ColumnInfo(defaultValue = "")
    val fullUrl: String,

    @ColumnInfo(defaultValue = "")
    val relativeUrl: String,

    @ColumnInfo(defaultValue = "")
    val host: String,

    @ColumnInfo(defaultValue = "")
    val httpRequestType: String,

    @ColumnInfo(defaultValue = "")
    val responseSummary: String? = null, // used to store response error message, overall summary etc

    @ColumnInfo(defaultValue = "")
    val requestHeaders: String? = null,

    @ColumnInfo(defaultValue = "")
    val requestBody: String? = null,

    @ColumnInfo(defaultValue = "")
    val responseHeaders: String? = null,

    @ColumnInfo(defaultValue = "")
    val responseBody: String? = null,

    @ColumnInfo(defaultValue = "0")
    val requestTimestamp: Long = 0L,

    @ColumnInfo(defaultValue = "")
    val readableRequestTime: String = "",

    @ColumnInfo(defaultValue = "0")
    val responseTimestamp: Long = 0L,

    @ColumnInfo(defaultValue = "0")
    val status: Int? = null,

    @ColumnInfo(defaultValue = "requestSize")
    val requestSize: String? = null,

    @ColumnInfo(defaultValue = "")
    val responseSize: String? = null,

    @ColumnInfo(defaultValue = "requestContentType")
    val requestContentType: String? = null,

    @ColumnInfo(defaultValue = "responseContentType")
    val responseContentType: String? = null,

    @ColumnInfo(defaultValue = "true")
    val inProgress: Boolean = true,

    @ColumnInfo(defaultValue = "false")
    val isSuccess: Boolean = false,

    @ColumnInfo(defaultValue = "httpMethod")
    val httpMethod: String,
)

@Serializable
data class NetworkRequestBody(
    val id: String,
    val requestBody: String,
    val requestContentType: String? = null
)

@Serializable
data class NetworkResponseHeaders(
    val id: String,
    val responseHeaders: String
)

@Serializable
data class NetworkResponseBody(
    val id: String,
    val isSuccess: Boolean,
    val responseSummary: String = "",
    val responseBody: String? = null,
    val status: Int? = null,
    val responseSize: String? = null,
    val responseTimestamp: Long = 0L,
    val responseContentType: String? = null,
    val inProgress: Boolean = false // Mark as not in progress anymore once the response comes back
)
