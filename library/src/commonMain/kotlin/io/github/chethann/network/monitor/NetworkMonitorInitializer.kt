package io.github.chethann.network.monitor

object NetworkMonitorInitializer {
    internal var context: Any? = null
    internal var bdDirectory: String? = null
    internal var appName: String? = null

    fun init(build: Builder.() -> Unit) {
        val builder = Builder().apply(build)
        this.context = builder.context
        this.bdDirectory = builder.bdDirectory
        this.appName = builder.appName
    }

    class Builder {
        var context: Any? = null
        var bdDirectory: String? = null
        var appName: String? = null
    }
}

