package io.github.chethann.network.monitor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import io.github.chethann.network.monitor.view.NetworkCallsView

fun MainViewController() = ComposeUIViewController { IOSView() }

@Composable
fun IOSView() {
    Column(modifier = Modifier.fillMaxSize()) {

        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f)) {
            App()
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            NetworkCallsView()
        }

    }


}
