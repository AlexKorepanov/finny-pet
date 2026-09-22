package ru.finny.petgame.economy

import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.economy.model.BudgetPlan
import ru.finny.petgame.economy.model.DepositResult
import ru.finny.petgame.economy.model.DistributionCheck
import ru.finny.petgame.economy.model.EarnResult
import ru.finny.petgame.economy.model.Earning
import ru.finny.petgame.economy.model.GoalEta
import ru.finny.petgame.economy.model.PlanCheckResult
import ru.finny.petgame.economy.model.PurchaseDraft
import ru.finny.petgame.economy.model.PurchaseRecord
import ru.finny.petgame.economy.model.PurchaseResult
import ru.finny.petgame.economy.model.SavingsBucket
import ru.finny.petgame.economy.model.WithdrawResult

class EconomyEngine {

    fun earn(state: EconomyState, source: String, amount: Long): EarnResult {
        if (source.isBlank()) {
            return EarnResult.Error(EXPLANATION_SOURCE_REQUIRED)
        }
        if (amount <= 0L) {
            return EarnResult.Error(EXPLANATION_AMOUNT_POSITIVE)
        }
        val newState = state.copy(balance = state.balance + amount)
        return EarnResult.Success(newState, Earning(source = source, amount = amount))
    }

    fun checkPlan(plan: BudgetPlan, available: Long): PlanCheckResult {
        val problems = mutableListOf<String>()
        BudgetPlan.BASE_DIRECTIONS.forEach { direction ->
            val amount = plan.amounts[direction]
            if (amount == null || amount == 0L) {
                problems += "Добавь деньги на: ${direction.label.lowercase()}."
            } else if (amount < 0L) {
                problems += "Сумма не может быть меньше нуля: ${direction.label.lowercase()}."
            }
        }
        val total = plan.amounts.values.sum()
        if (total > available) {
            problems += "План больше доступной суммы на ${total - available} монет. Уменьши распределение."
        }
        return if (problems.isEmpty()) {
            PlanCheckResult.Valid(plan = plan, remainder = available - total)
        } else {
            PlanCheckResult.Invalid(problems)
        }
    }

    fun purchase(state: EconomyState, draft: PurchaseDraft): PurchaseResult {
        if (draft.price <= 0L) {
            return PurchaseResult.Invalid(EXPLANATION_PRICE_POSITIVE)
        }
        if (state.balance < draft.price) {
            val shortfall = draft.price - state.balance
            return PurchaseResult.InsufficientFunds(
                balance = state.balance,
                price = draft.price,
                shortfall = shortfall,
                explanation = "Не хватает $shortfall монет, чтобы купить «${draft.title}». " +
                    "Можно выполнить задание, выбрать товар дешевле или отказаться, если покупка по желанию.",
            )
        }
        val newState = state.copy(balance = state.balance - draft.price)
        val record = PurchaseRecord(
            itemId = draft.itemId,
            title = draft.title,
            category = draft.category,
            price = draft.price,
        )
        return PurchaseResult.Success(newState, record)
    }

    fun depositToSavings(state: EconomyState, goalId: String, amount: Long): DepositResult {
        if (amount <= 0L) {
            return DepositResult.Invalid(EXPLANATION_AMOUNT_POSITIVE)
        }
        val bucket = state.savings[goalId]
            ?: return DepositResult.Invalid(EXPLANATION_GOAL_REQUIRED)
        if (state.balance < amount) {
            val shortfall = amount - state.balance
            return DepositResult.InsufficientFunds(
                balance = state.balance,
                shortfall = shortfall,
                explanation = "Не хватает ещё $shortfall, чтобы отложить. Отложи меньше или сначала подкопи баланс.",
            )
        }
        val newBucket = bucket.copy(savedAmount = bucket.savedAmount + amount)
        val newState = state.copy(
            balance = state.balance - amount,
            savings = state.savings + (goalId to newBucket),
        )
        return DepositResult.Success(newState, newBucket)
    }

    fun withdrawFromSavings(
        state: EconomyState,
        goalId: String,
        amount: Long,
        confirmed: Boolean,
        averageDeposit: Long = 0L,
    ): WithdrawResult {
        if (amount <= 0L) {
            return WithdrawResult.Invalid(EXPLANATION_AMOUNT_POSITIVE)
        }
        val bucket = state.savings[goalId]
            ?: return WithdrawResult.Invalid(EXPLANATION_GOAL_REQUIRED)
        if (amount > bucket.savedAmount) {
            return WithdrawResult.Invalid(
                "В копилке сейчас ${bucket.savedAmount}. Снять больше, чем накоплено, нельзя.",
            )
        }
        val newBucket = bucket.copy(savedAmount = bucket.savedAmount - amount)
        if (!confirmed) {
            return WithdrawResult.NeedsConfirmation(
                newSavedAmount = newBucket.savedAmount,
                eta = goalEta(newBucket, averageDeposit),
            )
        }
        val newState = state.copy(
            balance = state.balance + amount,
            savings = state.savings + (goalId to newBucket),
        )
        return WithdrawResult.Success(newState, newBucket)
    }

    fun goalEta(bucket: SavingsBucket, averageDeposit: Long): GoalEta {
        val remaining = bucket.remaining
        val periodsLeft: Int? = when {
            remaining == 0L -> 0
            averageDeposit <= 0L -> null
            else -> ((remaining + averageDeposit - 1) / averageDeposit).toInt()
        }
        return GoalEta(
            goalId = bucket.goalId,
            remaining = remaining,
            periodsLeft = periodsLeft,
            averageDeposit = averageDeposit,
        )
    }

    fun averageDeposit(deposits: List<Long>): Long =
        if (deposits.isEmpty()) 0L else deposits.sum() / deposits.size

    fun applyPetEffect(current: Int, delta: Int): Int = (current + delta).coerceIn(0, 100)

    fun checkDistribution(
        amounts: Map<BudgetDirection, Long>,
        sum: Long,
        minimums: Map<BudgetDirection, Long>,
    ): DistributionCheck {
        val problems = mutableListOf<String>()
        val total = amounts.values.sum()
        if (total != sum) {
            problems += "Распредели ровно $sum. Сейчас распределено $total."
        }
        BudgetDirection.entries.forEach { direction ->
            val amount = amounts[direction] ?: 0L
            if (amount < 0L) {
                problems += "Сумма не может быть меньше нуля: ${direction.label.lowercase()}."
            }
            minimums[direction]?.let { minimum ->
                if (amount < minimum) {
                    problems += "На ${direction.label.lowercase()} нужно минимум $minimum."
                }
            }
        }
        return if (problems.isEmpty()) {
            DistributionCheck.Valid(remainder = sum - total)
        } else {
            DistributionCheck.Invalid(problems)
        }
    }

    private companion object {
        const val EXPLANATION_SOURCE_REQUIRED = "У начисления должен быть источник."
        const val EXPLANATION_AMOUNT_POSITIVE = "Сумма должна быть больше нуля."
        const val EXPLANATION_PRICE_POSITIVE = "Цена должна быть больше нуля."
        const val EXPLANATION_GOAL_REQUIRED = "Сначала выбери цель накопления."
    }
}