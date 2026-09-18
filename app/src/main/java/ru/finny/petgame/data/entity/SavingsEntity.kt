package ru.finny.petgame.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "savings",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["profileId", "goalId"], unique = true)],
)
data class SavingsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val goalId: String,
    val goalTitle: String,
    val goalCost: Long,
    val savedAmount: Long,
    val createdAt: Long,
    val updatedAt: Long,
)