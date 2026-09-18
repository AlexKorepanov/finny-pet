package ru.finny.petgame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.finny.petgame.data.entity.ProfileEntity

@Dao
interface ProfileDao {

    @Insert
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("SELECT * FROM profiles ORDER BY id DESC LIMIT 1")
    suspend fun getCurrentProfile(): ProfileEntity?

    @Query("SELECT * FROM profiles ORDER BY id DESC LIMIT 1")
    fun observeCurrentProfile(): Flow<ProfileEntity?>

    @Query("DELETE FROM profiles")
    suspend fun deleteAll()
}