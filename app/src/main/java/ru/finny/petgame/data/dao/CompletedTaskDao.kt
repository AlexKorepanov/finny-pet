package ru.finny.petgame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.finny.petgame.data.entity.CompletedTaskEntity

@Dao
interface CompletedTaskDao {

    @Insert
    suspend fun insert(record: CompletedTaskEntity): Long

    @Query("SELECT * FROM completed_tasks WHERE profileId = :profileId ORDER BY completedAt DESC")
    fun observeByProfileId(profileId: Long): Flow<List<CompletedTaskEntity>>

    @Query("SELECT * FROM completed_tasks WHERE profileId = :profileId AND theme = :theme")
    suspend fun getByTheme(profileId: Long, theme: String): List<CompletedTaskEntity>

    @Query("DELETE FROM completed_tasks WHERE profileId = :profileId")
    suspend fun deleteByProfileId(profileId: Long)
}