package io.github.chethann.network.monitor

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.chethann.network.monitor.view.NetworkCallsView

fun main() = application {

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
