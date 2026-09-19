package ru.finny.petgame.data.model

sealed interface ShopPurchaseResult {
    data class Success(
        val title: String,
        val price: Long,
        val balance: Long,
        val mood: Int,
        val saturation: Int,
        val moodDelta: Int,
        val satietyDelta: Int,
    ) : ShopPurchaseResult

    data class InsufficientFunds(
        val balance: Long,
        val shortfall: Long,
        val explanation: String,
    ) : ShopPurchaseResult

    data class Invalid(val explanation: String) : ShopPurchaseResult
}