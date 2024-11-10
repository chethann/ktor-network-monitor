package io.github.chethann.network.monitor

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.chethann.network.monitor.view.NetworkCallsView

fun main() = application {

    NetworkMonitorInitializer.init {
        appName = "MyNetworkTest"
        bdDirectory = "${System.getProperty("java.io.tmpdir")}/db"
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "KotlinProject",
    ) {
        NetworkCallsView()
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "KotlinProject",
    ) {
        App()
    }
}
