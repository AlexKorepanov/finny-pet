package ru.finny.petgame.economy.model

data class SavingsBucket(
    val goalId: String,
    val goalTitle: String,
    val goalCost: Long,
    val savedAmount: Long,
) {
    val remaining: Long get() = maxOf(0L, goalCost - savedAmount)
    val isReached: Boolean get() = remaining == 0L
}

enum class SavingsOperationType(val label: String) {
    DEPOSIT("Пополнение"),
    WITHDRAW("Снятие"),
}