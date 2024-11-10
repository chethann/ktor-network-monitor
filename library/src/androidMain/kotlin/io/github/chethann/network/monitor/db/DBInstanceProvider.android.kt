package io.github.chethann.network.monitor.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.Dispatchers

actual fun provideDBInstance(context: Any?): NetworkMonitorDB {
    val androidApplicationContext = (context as? Context)?.applicationContext ?: throw RuntimeException("context can't be null")
    return createRoomDatabase(androidApplicationContext)
}

fun createRoomDatabase(context: Context): NetworkMonitorDB {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(NetworkMonitorDB.DATABASE_FILE_NAME)
    return Room.databaseBuilder<NetworkMonitorDB>(appContext, dbFile.absolutePath)
        .setDriver(AndroidSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}