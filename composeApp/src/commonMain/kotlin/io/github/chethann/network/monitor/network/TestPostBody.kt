package io.github.chethann.network.monitor.network

import kotlinx.serialization.Serializable

@Serializable
data class TestPostBody(
    val id: String,
    val fieldOne: String = "valueOne",
    val fieldTwo: Int = 100
)
