package ru.finny.petgame.ui.model

import ru.finny.petgame.data.entity.ProfileEntity

/**
 * Внешний вид единственного питомца — фенька.
 * Индексы совпадают с массивами строк в resources.
 */
data class PetLook(
    val hat: Int = 0,
    val face: Int = 0,
    val outfit: Int = 0,
    val emotion: Int = 0,
    val eyeColor: Int = 0,
) {
    fun withHat(value: Int) = copy(hat = value.coerceIn(0, HAT_COUNT - 1))
    fun withFace(value: Int) = copy(face = value.coerceIn(0, FACE_COUNT - 1))
    fun withOutfit(value: Int) = copy(outfit = value.coerceIn(0, OUTFIT_COUNT - 1))
    fun withEmotion(value: Int) = copy(emotion = value.coerceIn(0, EMOTION_COUNT - 1))
    fun withEyeColor(value: Int) = copy(eyeColor = value.coerceIn(0, EYE_COLOR_COUNT - 1))

    companion object {
        const val HAT_COUNT = 4
        const val FACE_COUNT = 4
        const val FESTIVE_OUTFIT = ProfileEntity.FESTIVE_OUTFIT
        const val OUTFIT_COUNT = 5
        const val EMOTION_COUNT = 4
        const val EYE_COLOR_COUNT = 4

        val Default = PetLook()
    }
}

fun ProfileEntity.toPetLook(): PetLook = PetLook(
    hat = petHat,
    face = petFace,
    outfit = petOutfit,
    emotion = petEmotion,
    eyeColor = petEyeColor,
)
