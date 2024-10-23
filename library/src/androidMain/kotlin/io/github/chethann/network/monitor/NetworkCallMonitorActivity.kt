package io.github.chethann.network.monitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.chethann.network.monitor.view.NetworkCallsView

class NetworkCallMonitorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NetworkCallsView()
        }
    }

}