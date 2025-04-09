package io.github.chethann.network.monitor.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkCallDao {
    @Query("SELECT * FROM networkCalls")
    suspend fun getAllNetworkCalls(): List<NetworkCallEntity>

    @Query("SELECT * FROM networkCalls ORDER BY requestTimestamp DESC")
    fun getAllNetworkCallsFlow(): Flow<List<NetworkCallEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addNetworkCall(networkCallEntity: NetworkCallEntity)

    @Update(entity = NetworkCallEntity::class)
    suspend fun updateNetworkCall(networkResponse: NetworkResponseBody)

    @Update(entity = NetworkCallEntity::class)
    suspend fun updateNetworkCall(networkResponse: NetworkRequestBody)

    @Update(entity = NetworkCallEntity::class)
    suspend fun updateNetworkCall(networkResponse: NetworkResponseHeaders)

    @Query("DELETE FROM networkCalls")
    suspend fun clearData()
}