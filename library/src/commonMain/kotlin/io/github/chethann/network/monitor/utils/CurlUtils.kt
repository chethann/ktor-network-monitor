package io.github.chethann.network.monitor.utils

import io.github.chethann.network.monitor.db.NetworkCallEntity

fun NetworkCallEntity.toCurlString(): String {
    val headersList = requestHeaders?.split("\n")?.toMutableList() ?: mutableListOf()

    // Add Content-Type header if not already present
    if (headersList.none { it.startsWith("Content-Type", ignoreCase = true) }) {
        headersList.add("Content-Type: application/json")
    }

    val headers = headersList.joinToString(" \\\n  ") { "--header \"$it\"" }

    val body = if (httpRequestType.equals("POST", ignoreCase = true) && requestBody?.isNotEmpty() == true) {
        requestBody.let { "--data '$it'" }
    } else {
        ""
    }
    var curl = """
        curl -X $httpRequestType \
        $headers \
        $body \
        "$fullUrl"
    """
        .replace("application/json; application/json", "application/json")
        .trimIndent()

    return curl
}