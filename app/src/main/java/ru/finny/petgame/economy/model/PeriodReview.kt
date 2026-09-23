package ru.finny.petgame.economy.model

data class PeriodReview(
    val requiredCovered: Boolean,
    val planMatched: Boolean,
    val savingsRegular: Boolean,
    val moodDelta: Int,
    val explanations: List<String>,
) {
    val isGoodPeriod: Boolean
        get() = requiredCovered && planMatched && savingsRegular
}
