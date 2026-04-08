package com.vnsas.vnappcall.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CallNoteDao {

    @Upsert
    suspend fun upsert(note: CallNote): Long

    @Delete
    suspend fun delete(note: CallNote)

    @Query("SELECT * FROM call_notes ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CallNote>>

    @Query(
        """
        SELECT * FROM call_notes
        WHERE timestamp >= :dayStart AND timestamp < :dayEnd
        ORDER BY timestamp DESC
        """
    )
    fun observeDay(dayStart: Long, dayEnd: Long): Flow<List<CallNote>>

    @Query(
        """
        SELECT * FROM call_notes
        WHERE timestamp >= :start AND timestamp < :end
        ORDER BY timestamp ASC
        """
    )
    suspend fun getBetween(start: Long, end: Long): List<CallNote>
}
