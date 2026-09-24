package ru.finny.petgame.data.model

import ru.finny.petgame.economy.model.PeriodReview

sealed interface PeriodCloseResult {
    data class Success(
        val periodIndex: Int,
        val review: PeriodReview,
        val previousMood: Int,
        val newMood: Int,
        val previousStage: Int,
        val newStage: Int,
        val stageGrew: Boolean,
        val periodIncome: Long,
        val newBalance: Long,
        val planBonus: Long = 0L,
        val previousSatiety: Int = 0,
        val newSatiety: Int = 0,
        val starsEarned: Int = 0,
        val totalStars: Int = 0,
        val starsToNextStage: Int? = null,
    ) : PeriodCloseResult

    data class Invalid(val explanation: String) : PeriodCloseResult
}
