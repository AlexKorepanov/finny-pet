package ru.finny.petgame.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerName: String,
    val petName: String,
    val petSpecies: Int,
    val petColor: Int,
    val createdAt: Long,
    val playerAvatar: Int = 0,
    val mood: Int = 70,
    val saturation: Int = 70,
)