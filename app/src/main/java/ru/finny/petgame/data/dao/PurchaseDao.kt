package ru.finny.petgame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.finny.petgame.data.entity.PurchaseEntity

@Dao
interface PurchaseDao {

    @Insert
    suspend fun insert(purchase: PurchaseEntity): Long

    @Query("SELECT * FROM purchases WHERE profileId = :profileId ORDER BY purchasedAt DESC")
    fun observeByProfileId(profileId: Long): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE profileId = :profileId AND periodIndex = :periodIndex ORDER BY purchasedAt")
    suspend fun getByPeriod(profileId: Long, periodIndex: Int): List<PurchaseEntity>

    @Query("DELETE FROM purchases WHERE profileId = :profileId")
    suspend fun deleteByProfileId(profileId: Long)
}