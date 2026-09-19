package ru.finny.petgame.content.model

import ru.finny.petgame.economy.model.PurchaseCategory

data class ShopItemContent(
    val id: String,
    val title: String,
    val category: PurchaseCategory,
    val price: Long,
    val effect: String,
    val moodDelta: Int = 0,
)