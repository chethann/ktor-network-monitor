package io.github.chethann.network.monitor.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import io.github.chethann.network.monitor.enum.Platform
import io.github.chethann.network.monitor.enum.getPlatform


// Function to check if the platform is Android
fun isAndroid(): Boolean = getPlatform() == Platform.ANDROID

// Function to check if the platform is iOS
fun isIOS(): Boolean = getPlatform() == Platform.IOS

@Composable
fun UnifiedBackHandler(onBack: () -> Unit) {
    PlatformSpecificBackHandler(onBack)
}