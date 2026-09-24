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
import ru.finny.petgame.economy.model.WeekNeed
import ru.finny.petgame.economy.model.WithdrawResult

private fun Long.coins(): String {
    val mod100 = this % 100
    val mod10 = this % 10
    val word = when {
        mod100 in 11..14 -> "монет"
        mod10 == 1L -> "монету"
        mod10 in 2..4 -> "монеты"
        else -> "монет"
    }
    return "$this $word"
}

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
                etaBefore = goalEta(bucket, averageDeposit),
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

    /**
     * Среднее пополнение за неделю: сумма пополнений / число недель
     * от первого пополнения до текущей недели включительно.
     */
    fun averageDepositPerPeriod(deposits: List<Pair<Int, Long>>, currentPeriodIndex: Int): Long {
        if (deposits.isEmpty()) return 0L
        val firstPeriod = deposits.minOf { it.first }
        val periods = (currentPeriodIndex - firstPeriod + 1).coerceAtLeast(1)
        val total = deposits.sumOf { it.second }
        return (total / periods).coerceAtLeast(if (total > 0L) 1L else 0L)
    }

    fun applyPetEffect(current: Int, delta: Int): Int = (current + delta).coerceIn(PET_STAT_MIN, PET_STAT_MAX)

    /** Полная награда с первой попытки, половина (с округлением вверх) — после ошибки. */
    fun taskReward(baseReward: Long, previousWrongAttempts: Int): Long =
        if (previousWrongAttempts <= 0) baseReward else (baseReward + 1L) / 2L

    /** На сколько покупка выйдет за план конверта (0 — если в пределах плана). */
    fun overPlanAmount(plan: Map<BudgetDirection, Long>, fact: Map<BudgetDirection, Long>, direction: BudgetDirection, price: Long): Long {
        val planned = plan[direction] ?: 0L
        val spent = fact[direction] ?: 0L
        return (spent + price - planned).coerceAtLeast(0L)
    }

    fun evaluatePeriod(
        plan: Map<BudgetDirection, Long>,
        fact: Map<BudgetDirection, Long>,
        weekNeeds: List<WeekNeed> = emptyList(),
        boughtItemIds: Set<String> = emptySet(),
    ): PeriodReview {
        val planRequired = plan[BudgetDirection.REQUIRED] ?: 0L
        val planOptional = plan[BudgetDirection.OPTIONAL] ?: 0L
        val planSavings = plan[BudgetDirection.SAVINGS] ?: 0L
        val factRequired = fact[BudgetDirection.REQUIRED] ?: 0L
        val factOptional = fact[BudgetDirection.OPTIONAL] ?: 0L
        val factSavings = fact[BudgetDirection.SAVINGS] ?: 0L

        val missingNeeds = weekNeeds.filter { it.itemId !in boughtItemIds }.map { it.title }
        val requiredCovered = if (weekNeeds.isEmpty()) factRequired > 0L else missingNeeds.isEmpty()
        val requiredOver = (factRequired - planRequired).coerceAtLeast(0L)
        val optionalOver = (factOptional - planOptional).coerceAtLeast(0L)
        val savingsShort = (planSavings - factSavings).coerceAtLeast(0L)
        val planMatched = requiredOver == 0L && optionalOver == 0L && savingsShort == 0L
        val savingsRegular = factSavings > 0L
        val economized = (planRequired - factRequired).coerceAtLeast(0L) +
            (planOptional - factOptional).coerceAtLeast(0L)
        val needsCost = weekNeeds.sumOf { it.price }

        var moodDelta = MOOD_WEEK_DECAY
        val explanations = mutableListOf<String>()
        if (requiredCovered) {
            explanations += "Всё нужное куплено — Финни сыт и ухожен."
        } else {
            moodDelta += MOOD_REQUIRED_MISS
            explanations += if (missingNeeds.isEmpty()) {
                "На этой неделе Финни не получил нужного (еда и уход)."
            } else {
                "Не хватило нужного: ${missingNeeds.joinToString()}."
            }
        }
        if (planMatched) {
            moodDelta += MOOD_PLAN_OK
            explanations += if (economized > 0L) {
                "Ты тратил по плану и даже сэкономил ${economized.coins()}."
            } else {
                "Ты потратил ровно столько, сколько запланировал."
            }
        } else {
            if (requiredOver > 0L) explanations += "На нужное потрачено на ${requiredOver.coins()} больше плана."
            if (optionalOver > 0L) explanations += "На покупки по желанию потрачено на ${optionalOver.coins()} больше плана."
            if (savingsShort > 0L) explanations += "В копилку отложено на ${savingsShort.coins()} меньше плана."
        }
        if (savingsRegular) {
            moodDelta += MOOD_SAVINGS_OK
            explanations += "Копилка пополнена — мечта стала ближе."
        } else {
            explanations += "В копилку ничего не попало. Даже 1 монета — уже шаг к мечте."
        }
        explanations += "За неделю Финни проголодался и немного соскучился — это нормально, позаботься о нём снова."

        val advice = when {
            !requiredCovered && needsCost > planRequired ->
                "Нужное на неделю стоит ${needsCost.coins()} — запланируй на нужное не меньше."
            !requiredCovered -> "Сначала купи всё из списка «Нужно Финни», а потом — по желанию."
            optionalOver > 0L -> "Перед покупкой по желанию посмотри, сколько осталось в плане."
            requiredOver > 0L -> "Сравни цены нужного с планом, прежде чем покупать."
            savingsShort > 0L || !savingsRegular -> "Отложи в копилку сразу после получения монет — так проще не потратить."
            else -> "Так держать! Попробуй отложить в копилку чуть больше."
        }
        return PeriodReview(
            requiredCovered = requiredCovered,
            planMatched = planMatched,
            savingsRegular = savingsRegular,
            moodDelta = moodDelta,
            explanations = explanations,
            satietyDelta = SATIETY_WEEK_DECAY,
            missingNeeds = missingNeeds,
            economized = economized,
            advice = advice,
        )
    }

    /** Стадия зависит от звёздочек за все недели и никогда не уменьшается. */
    fun stageForStars(totalStars: Int): Int = when {
        totalStars >= STARS_FOR_ADULT -> 2
        totalStars >= STARS_FOR_TEEN -> 1
        else -> 0
    }

    /** Сколько звёздочек осталось до следующей стадии (null — стадия максимальная). */
    fun starsToNextStage(totalStars: Int): Int? = when {
        totalStars < STARS_FOR_TEEN -> STARS_FOR_TEEN - totalStars
        totalStars < STARS_FOR_ADULT -> STARS_FOR_ADULT - totalStars
        else -> null
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
        const val STARS_FOR_TEEN = 4
        const val STARS_FOR_ADULT = 9
        const val PET_STAT_MIN = 10
        const val PET_STAT_MAX = 100
        const val PERIOD_INCOME_AMOUNT = 30L
        const val PERIOD_INCOME_SOURCE = "Карманные монеты на неделю"
        const val PLAN_BONUS_AMOUNT = 5L
        const val PLAN_BONUS_SOURCE = "Бонус за выполненный план"
        const val MOOD_WEEK_DECAY = -10
        const val SATIETY_WEEK_DECAY = -30
        private const val MOOD_REQUIRED_MISS = -8
        private const val MOOD_PLAN_OK = 5
        private const val MOOD_SAVINGS_OK = 5
        private const val EXPLANATION_SOURCE_REQUIRED = "У начисления должен быть источник."
        private const val EXPLANATION_AMOUNT_POSITIVE = "Сумма должна быть больше нуля."
        private const val EXPLANATION_PRICE_POSITIVE = "Цена должна быть больше нуля."
        private const val EXPLANATION_GOAL_REQUIRED = "Сначала выбери цель накопления."
    }
}