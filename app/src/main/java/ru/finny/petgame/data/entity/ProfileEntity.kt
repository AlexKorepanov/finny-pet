package ru.finny.petgame.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerName: String,
    val petName: String,
    val petHat: Int = 0,
    val petFace: Int = 0,
    val petOutfit: Int = 0,
    val petEmotion: Int = 0,
    val petEyeColor: Int = 0,
    val createdAt: Long,
    val playerAvatar: Int = 0,
    val mood: Int = 70,
    val saturation: Int = 70,
    val petStage: Int = 0,
    val goodPeriods: Int = 0,
    val growthStars: Int = 0,
    val allTasksOpen: Boolean = false,
) {
    companion object {
        /** Праздничный наряд из копилки. Совпадает с индексом в PetLook. */
        const val FESTIVE_OUTFIT = 4
    }
}
