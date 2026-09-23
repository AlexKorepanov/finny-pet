package ru.finny.petgame.economy

import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.economy.model.BudgetPlan
import ru.finny.petgame.economy.model.DepositResult
import ru.finny.petgame.economy.model.DistributionCheck
import ru.finny.petgame.economy.model.EarnResult
import ru.finny.petgame.economy.model.Earning
import ru.finny.petgame.economy.model.GoalEta
import ru.finny.petgame.economy.model.PeriodReview
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

    fun evaluatePeriod(
        plan: Map<BudgetDirection, Long>,
        fact: Map<BudgetDirection, Long>,
    ): PeriodReview {
        val planRequired = plan[BudgetDirection.REQUIRED] ?: 0L
        val planOptional = plan[BudgetDirection.OPTIONAL] ?: 0L
        val planSavings = plan[BudgetDirection.SAVINGS] ?: 0L
        val factRequired = fact[BudgetDirection.REQUIRED] ?: 0L
        val factOptional = fact[BudgetDirection.OPTIONAL] ?: 0L
        val factSavings = fact[BudgetDirection.SAVINGS] ?: 0L

        val requiredCovered = factRequired >= planRequired && planRequired > 0L
        val planMatched = factRequired <= planRequired &&
            factOptional <= planOptional &&
            factSavings >= planSavings
        val savingsRegular = factSavings > 0L

        var moodDelta = 0
        val explanations = mutableListOf<String>()
        if (requiredCovered) {
            moodDelta += MOOD_REQUIRED_OK
            explanations += "Нужное куплено — питомцу спокойнее."
        } else {
            moodDelta += MOOD_REQUIRED_MISS
            explanations += "Нужного не хватило. В следующий раз купи еду и уход по плану."
        }
        if (planMatched) {
            moodDelta += MOOD_PLAN_OK
            explanations += "Факт совпал с планом — хорошая привычка."
        } else {
            moodDelta += MOOD_PLAN_MISS
            explanations += "Факт не совпал с планом. Посмотри, где потратил больше или меньше."
        }
        if (savingsRegular) {
            moodDelta += MOOD_SAVINGS_OK
            explanations += "Ты отложил монеты в копилку — это помогает цели."
        } else {
            explanations += "В этом периоде в копилку ничего не попало. Можно отложить чуть-чуть в следующий раз."
        }
        return PeriodReview(
            requiredCovered = requiredCovered,
            planMatched = planMatched,
            savingsRegular = savingsRegular,
            moodDelta = moodDelta,
            explanations = explanations,
        )
    }

    fun nextPetStage(currentStage: Int, isGoodPeriod: Boolean): Int {
        if (!isGoodPeriod) return currentStage.coerceIn(0, MAX_PET_STAGE)
        return (currentStage + 1).coerceAtMost(MAX_PET_STAGE)
    }

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

    companion object {
        const val MAX_PET_STAGE = 2
        const val PERIOD_INCOME_AMOUNT = 20L
        const val PERIOD_INCOME_SOURCE = "Доход за новый период"
        private const val MOOD_REQUIRED_OK = 8
        private const val MOOD_REQUIRED_MISS = -8
        private const val MOOD_PLAN_OK = 5
        private const val MOOD_PLAN_MISS = -3
        private const val MOOD_SAVINGS_OK = 5
        private const val EXPLANATION_SOURCE_REQUIRED = "У начисления должен быть источник."
        private const val EXPLANATION_AMOUNT_POSITIVE = "Сумма должна быть больше нуля."
        private const val EXPLANATION_PRICE_POSITIVE = "Цена должна быть больше нуля."
        private const val EXPLANATION_GOAL_REQUIRED = "Сначала выбери цель накопления."
    }
}