package ru.finny.petgame.data.model

sealed interface GoalAchieveResult {
    data class Success(
        val goalTitle: String,
        val moodDelta: Int,
        val mood: Int,
        val savedLeft: Long,
    ) : GoalAchieveResult

    data class Invalid(val explanation: String) : GoalAchieveResult
}