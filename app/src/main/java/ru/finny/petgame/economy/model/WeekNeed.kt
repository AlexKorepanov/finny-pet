package ru.finny.petgame.economy.model

/** Товар из списка «Что нужно Финни на неделе». */
data class WeekNeed(
    val itemId: String,
    val title: String,
    val price: Long,
)
