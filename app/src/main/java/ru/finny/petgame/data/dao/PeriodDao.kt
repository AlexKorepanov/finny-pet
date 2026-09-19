package ru.finny.petgame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.finny.petgame.data.entity.PeriodEntity

@Dao
interface PeriodDao {

    @Insert
    suspend fun insert(period: PeriodEntity): Long

    @Update
    suspend fun update(period: PeriodEntity)

    @Query("SELECT * FROM periods WHERE profileId = :profileId AND periodIndex = :periodIndex LIMIT 1")
    suspend fun getByIndex(profileId: Long, periodIndex: Int): PeriodEntity?

    @Query("SELECT * FROM periods WHERE profileId = :profileId ORDER BY periodIndex DESC LIMIT 1")
    suspend fun getLatest(profileId: Long): PeriodEntity?

    @Query("SELECT * FROM periods WHERE profileId = :profileId ORDER BY periodIndex")
    fun observeByProfileId(profileId: Long): Flow<List<PeriodEntity>>

    @Query("DELETE FROM periods WHERE profileId = :profileId")
    suspend fun deleteByProfileId(profileId: Long)
}