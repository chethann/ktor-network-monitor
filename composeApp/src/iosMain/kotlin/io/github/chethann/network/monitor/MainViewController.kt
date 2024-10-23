package io.github.chethann.network.monitor

import androidx.compose.ui.window.ComposeUIViewController
import io.github.chethann.network.monitor.view.NetworkCallsView

fun MainViewController() = ComposeUIViewController { NetworkCallsView() }