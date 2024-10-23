package io.github.chethann.network.monitor.db

import io.github.chethann.network.monitor.NetworkMonitorInitializer

internal object DBInstanceProvider {
    private lateinit var _networkMonitorDB: NetworkMonitorDB

    fun getNetworkMonitorDB(): NetworkMonitorDB {
        if(!::_networkMonitorDB.isInitialized) {
            _networkMonitorDB = provideDBInstance(NetworkMonitorInitializer.context)
        }
        return _networkMonitorDB
    }
}

expect fun provideDBInstance(context: Any? = null): NetworkMonitorDB