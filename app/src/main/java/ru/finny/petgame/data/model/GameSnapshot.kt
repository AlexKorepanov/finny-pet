package ru.finny.petgame.data.model

import ru.finny.petgame.data.entity.BudgetPlanItemEntity
import ru.finny.petgame.data.entity.PeriodEntity
import ru.finny.petgame.data.entity.ProfileEntity
import ru.finny.petgame.data.entity.SavingsEntity

data class GameSnapshot(
    val profile: ProfileEntity,
    val balance: Long,
    val savings: List<SavingsEntity>,
    val savingsTotal: Long,
    val selectedGoalId: String?,
    val selectedGoalTitle: String?,
    val currentPeriod: PeriodEntity?,
    val plan: List<BudgetPlanItemEntity>,
)