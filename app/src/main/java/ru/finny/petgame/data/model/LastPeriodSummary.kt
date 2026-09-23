package ru.finny.petgame.data.model

import ru.finny.petgame.economy.model.BudgetDirection

data class LastPeriodSummary(
    val periodIndex: Int,
    val plan: Map<BudgetDirection, Long>,
    val fact: Map<BudgetDirection, Long>,
    val closedAt: Long?,
)
