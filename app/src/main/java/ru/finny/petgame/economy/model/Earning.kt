package ru.finny.petgame.economy.model

import ru.finny.petgame.economy.EconomyState

data class Earning(
    val source: String,
    val amount: Long,
)

sealed interface EarnResult {
    data class Success(val state: EconomyState, val earning: Earning) : EarnResult
    data class Error(val explanation: String) : EarnResult
}