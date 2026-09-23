package ru.finny.petgame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.finny.petgame.data.entity.BalanceEntity

@Dao
interface BalanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(balance: BalanceEntity)

    @Query("SELECT * FROM balance WHERE profileId = :profileId")
    suspend fun getByProfileId(profileId: Long): BalanceEntity?

    @Query("SELECT * FROM balance WHERE profileId = :profileId")
    fun observeByProfileId(profileId: Long): Flow<BalanceEntity?>

    @Query("DELETE FROM balance WHERE profileId = :profileId")
    suspend fun deleteByProfileId(profileId: Long)

    @Query("DELETE FROM balance")
    suspend fun deleteAll()
}