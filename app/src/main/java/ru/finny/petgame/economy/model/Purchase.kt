package ru.finny.petgame.economy.model

import ru.finny.petgame.economy.EconomyState

data class PurchaseDraft(
    val itemId: String,
    val title: String,
    val category: PurchaseCategory,
    val price: Long,
)

data class PurchaseRecord(
    val itemId: String,
    val title: String,
    val category: PurchaseCategory,
    val price: Long,
)

sealed interface PurchaseResult {
    data class Success(val state: EconomyState, val purchase: PurchaseRecord) : PurchaseResult

    data class InsufficientFunds(
        val balance: Long,
        val price: Long,
        val shortfall: Long,
        val explanation: String,
    ) : PurchaseResult

    data class Invalid(val explanation: String) : PurchaseResult
}