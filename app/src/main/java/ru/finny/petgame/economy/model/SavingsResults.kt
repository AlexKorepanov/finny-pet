package ru.finny.petgame.economy.model

import ru.finny.petgame.economy.EconomyState

sealed interface DepositResult {
    data class Success(val state: EconomyState, val bucket: SavingsBucket) : DepositResult

    data class InsufficientFunds(
        val balance: Long,
        val shortfall: Long,
        val explanation: String,
    ) : DepositResult

    data class Invalid(val explanation: String) : DepositResult
}

sealed interface WithdrawResult {
    data class NeedsConfirmation(
        val newSavedAmount: Long,
        val eta: GoalEta,
    ) : WithdrawResult

    data class Success(val state: EconomyState, val bucket: SavingsBucket) : WithdrawResult

    data class Invalid(val explanation: String) : WithdrawResult
}