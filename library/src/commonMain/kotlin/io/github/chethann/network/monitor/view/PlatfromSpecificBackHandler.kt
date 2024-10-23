package io.github.chethann.network.monitor.view

import androidx.compose.runtime.Composable
import io.github.chethann.network.monitor.enum.Platform

@Composable
expect fun PlatformSpecificBackHandler(onBack: () -> Unit)