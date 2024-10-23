package io.github.chethann.network.monitor.enum

internal enum class Platform {
    ANDROID,
    IOS,
    DESKTOP
}

internal expect fun getPlatform(): Platform