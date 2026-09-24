package ru.finny.petgame.economy.model

data class PeriodReview(
    val requiredCovered: Boolean,
    val planMatched: Boolean,
    val savingsRegular: Boolean,
    val moodDelta: Int,
    val explanations: List<String>,
    /** Изменение сытости за неделю (Финни ест каждый день). */
    val satietyDelta: Int = 0,
    /** Что из нужного недели так и не куплено. */
    val missingNeeds: List<String> = emptyList(),
    /** Сколько монет из плана трат осталось неистраченными. */
    val economized: Long = 0L,
    /** Один конкретный совет на следующую неделю. */
    val advice: String = "",
) {
    val isGoodPeriod: Boolean
        get() = requiredCovered && planMatched && savingsRegular

    /** Звёздочки роста: по одной за каждую пройденную проверку. */
    val stars: Int
        get() = listOf(requiredCovered, planMatched, savingsRegular).count { it }
}
