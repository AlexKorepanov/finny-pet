package ru.finny.petgame.content.model

/** Учебная неделя: тема, короткий урок и список нужного для Финни (id товаров из shop.json). */
data class WeekContent(
    val index: Int,
    val theme: String,
    val lesson: String,
    val needs: List<String>,
)
