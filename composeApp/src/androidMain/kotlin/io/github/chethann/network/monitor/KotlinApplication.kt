package io.github.chethann.network.monitor

import android.app.Application

class KotlinApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        initNetworkCallLogger()
    }

    private fun initNetworkCallLogger() {
        NetworkMonitorInitializer.init {
            context = this@KotlinApplication
        }
    }
}