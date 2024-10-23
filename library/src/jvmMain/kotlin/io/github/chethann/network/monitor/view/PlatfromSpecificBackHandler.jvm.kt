package io.github.chethann.network.monitor.view

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp

@Composable
actual fun PlatformSpecificBackHandler(onBack: () -> Unit) {
    Box( modifier =
    Modifier.onPreviewKeyEvent { keyEvent ->
        println("In onPreviewKeyEvent")
        if (keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Key.Escape) {
            onBack()
            true
        } else {
            false
        }
    }.focusable()
        .size(1.dp)
    )
}