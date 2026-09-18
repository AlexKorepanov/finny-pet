package ru.finny.petgame.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "balance")
data class BalanceEntity(
    @PrimaryKey val profileId: Long,
    val amount: Long,
    val updatedAt: Long,
)