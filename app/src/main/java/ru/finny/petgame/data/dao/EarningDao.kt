package ru.finny.petgame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.finny.petgame.data.entity.EarningEntity

@Dao
interface EarningDao {

    @Insert
    suspend fun insert(earning: EarningEntity): Long

    @Query("SELECT * FROM earnings WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun observeByProfileId(profileId: Long): Flow<List<EarningEntity>>

    @Query("SELECT * FROM earnings WHERE profileId = :profileId AND periodIndex = :periodIndex ORDER BY createdAt")
    suspend fun getByPeriod(profileId: Long, periodIndex: Int): List<EarningEntity>

    @Query("DELETE FROM earnings WHERE profileId = :profileId")
    suspend fun deleteByProfileId(profileId: Long)
}