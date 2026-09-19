package ru.finny.petgame.economy.model

import ru.finny.petgame.economy.EconomyState

data class BudgetPlan(val amounts: Map<BudgetDirection, Long>) {
    val total: Long get() = amounts.values.sum()

    companion object {
        val BASE_DIRECTIONS: List<BudgetDirection> = BudgetDirection.entries
    }
}

sealed interface PlanCheckResult {
    data class Valid(val plan: BudgetPlan, val remainder: Long) : PlanCheckResult
    data class Invalid(val problems: List<String>) : PlanCheckResult
}