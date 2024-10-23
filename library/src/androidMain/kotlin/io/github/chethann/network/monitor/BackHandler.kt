package io.github.chethann.network.monitor

import androidx.compose.runtime.Composable

@Composable
fun PlatformSpecificBackHandler(onBack: () -> Unit) {
    androidx.activity.compose.BackHandler {

    }
}