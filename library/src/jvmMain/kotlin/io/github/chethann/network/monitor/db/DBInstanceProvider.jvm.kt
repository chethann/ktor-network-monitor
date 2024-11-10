package io.github.chethann.network.monitor.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.chethann.network.monitor.NetworkMonitorInitializer
import java.io.File

actual fun provideDBInstance(context: Any?): NetworkMonitorDB {
    val dbDirectory = NetworkMonitorInitializer.bdDirectory ?: getStorageDirectory(NetworkMonitorInitializer.appName ?: "networkMonitor")
    val dbFile = File(dbDirectory, NetworkMonitorDB.DATABASE_FILE_NAME)
    return Room.databaseBuilder<NetworkMonitorDB>(
        name = dbFile.absolutePath,
    ).setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
        .build()
}

private fun getStorageDirectory(leafDirectoryName: String): String {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> "${System.getenv("LOCALAPPDATA")}/$leafDirectoryName"
        os.contains("mac") -> "${System.getProperty("user.home")}/Library/Application Support/$leafDirectoryName"
        else -> System.getProperty("user.home") + "/.local/share/$leafDirectoryName"
    }
}