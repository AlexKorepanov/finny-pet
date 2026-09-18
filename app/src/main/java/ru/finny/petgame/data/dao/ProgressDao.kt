package ru.finny.petgame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.finny.petgame.data.entity.ProgressEntity

@Dao
interface ProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: ProgressEntity)

    @Query("SELECT * FROM progress WHERE profileId = :profileId")
    fun observeByProfileId(profileId: Long): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE profileId = :profileId AND kind = :kind")
    suspend fun getByKind(profileId: Long, kind: String): List<ProgressEntity>

    @Query("DELETE FROM progress WHERE profileId = :profileId")
    suspend fun deleteByProfileId(profileId: Long)
}