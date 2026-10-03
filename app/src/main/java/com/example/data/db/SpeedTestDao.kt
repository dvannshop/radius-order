package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedTestDao {
    @Query("SELECT * FROM speed_test_records ORDER BY timestamp DESC LIMIT 50")
    fun getAllRecords(): Flow<List<SpeedTestRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SpeedTestRecord): Long

    @Query("DELETE FROM speed_test_records")
    suspend fun clearAllRecords()
}
