package ru.finny.petgame.economy.model

data class GoalEta(
    val goalId: String,
    val remaining: Long,
    val periodsLeft: Int?,
    val averageDeposit: Long,
)