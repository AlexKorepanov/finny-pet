package ru.finny.petgame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.finny.petgame.data.entity.SavingsEntity

@Dao
interface SavingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(savings: SavingsEntity): Long

    @Update
    suspend fun update(savings: SavingsEntity)

    @Query("SELECT * FROM savings WHERE profileId = :profileId ORDER BY updatedAt DESC")
    fun observeByProfileId(profileId: Long): Flow<List<SavingsEntity>>

    @Query("SELECT * FROM savings WHERE profileId = :profileId ORDER BY updatedAt DESC")
    suspend fun getByProfileId(profileId: Long): List<SavingsEntity>

    @Query("SELECT * FROM savings WHERE profileId = :profileId AND goalId = :goalId LIMIT 1")
    suspend fun getByGoal(profileId: Long, goalId: String): SavingsEntity?

    @Query("DELETE FROM savings WHERE profileId = :profileId")
    suspend fun deleteByProfileId(profileId: Long)
}