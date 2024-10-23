package io.github.chethann.network.monitor.view

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun PlatformSpecificBackHandler(onBack: () -> Unit) {

    BackHandler {
        onBack()
    }
}