package ru.finny.petgame.content.model

import ru.finny.petgame.economy.model.PurchaseCategory

data class ShopItemContent(
    val id: String,
    val title: String,
    val category: PurchaseCategory,
    val price: Long,
    val effect: String,
    val moodDelta: Int = 0,
    val satietyDelta: Int = 0,
    /** Картинка товара в магазине; если в контенте не задана — корзинка. */
    val emoji: String = DEFAULT_EMOJI,
) {
    companion object {
        const val DEFAULT_EMOJI = "🛒"
    }
}