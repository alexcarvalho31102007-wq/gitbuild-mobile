package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BuildHistoryDao {
    @Query("SELECT * FROM build_history ORDER BY createdAt DESC")
    fun getAll(): Flow<List<BuildHistoryEntity>>

    @Query("SELECT * FROM build_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BuildHistoryEntity?

    @Query("SELECT * FROM build_history WHERE status IN ('QUEUED', 'IN_PROGRESS') ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveBuild(): BuildHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BuildHistoryEntity): Long

    @Update
    suspend fun update(entity: BuildHistoryEntity)

    @Delete
    suspend fun delete(entity: BuildHistoryEntity)

    @Query("DELETE FROM build_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
