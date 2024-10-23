package io.github.chethann.network.monitor

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform