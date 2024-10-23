package io.github.chethann.network.monitor.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

actual fun provideDBInstance(context: Any?): NetworkMonitorDB {
    val dbFile = File("db", NetworkMonitorDB.DATABASE_FILE_NAME)
    return Room.databaseBuilder<NetworkMonitorDB>(
        name = dbFile.absolutePath,
    ).setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
        .build()
}