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
    ) : PeriodCloseResult

    data class Invalid(val explanation: String) : PeriodCloseResult
}
