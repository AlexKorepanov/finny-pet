package ru.finny.petgame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.finny.petgame.data.entity.SavingsOperationEntity

@Dao
interface SavingsOperationDao {

    @Insert
    suspend fun insert(operation: SavingsOperationEntity): Long

    @Query("SELECT * FROM savings_operations WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun observeByProfileId(profileId: Long): Flow<List<SavingsOperationEntity>>

    @Query("SELECT * FROM savings_operations WHERE profileId = :profileId ORDER BY createdAt, id")
    suspend fun getByProfileId(profileId: Long): List<SavingsOperationEntity>

    @Query("SELECT * FROM savings_operations WHERE profileId = :profileId AND goalId = :goalId AND type = 'DEPOSIT' ORDER BY createdAt")
    suspend fun getDepositsByGoal(profileId: Long, goalId: String): List<SavingsOperationEntity>

    @Query("SELECT * FROM savings_operations WHERE profileId = :profileId AND periodIndex = :periodIndex AND type = :type ORDER BY createdAt")
    suspend fun getByPeriod(profileId: Long, periodIndex: Int, type: String): List<SavingsOperationEntity>

    @Query("DELETE FROM savings_operations WHERE profileId = :profileId")
    suspend fun deleteByProfileId(profileId: Long)
}