package ru.finny.petgame.data.model

sealed interface TaskCompletionResult {
    data class Success(
        val isCorrect: Boolean,
        val reward: Long,
        val balance: Long,
        val mood: Int,
        val moodDelta: Int,
        /** Верно с первой попытки (без ошибок до этого). */
        val firstTry: Boolean = true,
        /** Награда, которую давали бы с первой попытки. */
        val fullReward: Long = reward,
    ) : TaskCompletionResult

    data object AlreadyCompleted : TaskCompletionResult

    data class Invalid(val explanation: String) : TaskCompletionResult
}
