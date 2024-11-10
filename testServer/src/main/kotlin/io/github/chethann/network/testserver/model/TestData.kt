package io.github.chethann.network.testserver.model
import kotlinx.serialization.Serializable

@Serializable
data class TestData(
    val uuid: String,
    val someFiled: String = "Some Data",
    val someObject: SomeObject = SomeObject()
)

@Serializable
data class SomeObject(
    val someFiled: String = "Some Data"
)
