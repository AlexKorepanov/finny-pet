package ru.finny.petgame.data.model

sealed interface TaskCompletionResult {
    data class Success(
        val isCorrect: Boolean,
        val reward: Long,
        val balance: Long,
        val mood: Int,
        val moodDelta: Int,
    ) : TaskCompletionResult

    data object AlreadyCompleted : TaskCompletionResult

    data class Invalid(val explanation: String) : TaskCompletionResult
}