package io.github.chethann.network.monitor.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import io.github.chethann.network.monitor.db.NetworkMonitorDB.Companion.DATABASE_VERSION

@Database(
    entities = [
        NetworkCallEntity::class
    ],
    version = DATABASE_VERSION
)
@ConstructedBy(NetworkMonitorDatabaseConstructor::class)
abstract class NetworkMonitorDB() : RoomDatabase() {

    abstract fun getNetworkCallDao(): NetworkCallDao

    companion object {
        internal const val DATABASE_VERSION = 3
        internal const val DATABASE_FILE_NAME = "network_monitor.db"
    }
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object NetworkMonitorDatabaseConstructor : RoomDatabaseConstructor<NetworkMonitorDB> {
    override fun initialize(): NetworkMonitorDB
}