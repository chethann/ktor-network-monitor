package io.github.chethann.network.monitor

object NetworkMonitorInitializer {
    var context: Any? = null

    fun init(context: Any? = null) {
        this.context = context
    }
}