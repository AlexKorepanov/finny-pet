package ru.finny.petgame.data.repository

import androidx.room.withTransaction
import ru.finny.petgame.data.PetDatabase
import ru.finny.petgame.data.entity.BalanceEntity
import ru.finny.petgame.data.entity.BudgetPlanItemEntity
import ru.finny.petgame.data.entity.CompletedTaskEntity
import ru.finny.petgame.data.entity.EarningEntity
import ru.finny.petgame.data.entity.PeriodEntity
import ru.finny.petgame.data.entity.ProfileEntity
import ru.finny.petgame.data.entity.ProgressEntity
import ru.finny.petgame.data.entity.PurchaseEntity
import ru.finny.petgame.data.entity.SavingsEntity
import ru.finny.petgame.data.entity.SavingsOperationEntity
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.data.model.GoalAchieveResult
import ru.finny.petgame.data.model.PeriodCloseResult
import ru.finny.petgame.data.model.PeriodStatus
import ru.finny.petgame.data.model.ShopPurchaseResult
import ru.finny.petgame.data.model.TaskCompletionResult
import ru.finny.petgame.economy.EconomyEngine
import ru.finny.petgame.economy.EconomyState
import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.economy.model.BudgetPlan
import ru.finny.petgame.economy.model.DepositResult
import ru.finny.petgame.economy.model.DistributionCheck
import ru.finny.petgame.economy.model.EarnResult
import ru.finny.petgame.economy.model.GoalEta
import ru.finny.petgame.economy.model.PeriodReview
import ru.finny.petgame.economy.model.PlanCheckResult
import ru.finny.petgame.economy.model.PurchaseCategory
import ru.finny.petgame.economy.model.PurchaseDraft
import ru.finny.petgame.economy.model.PurchaseResult
import ru.finny.petgame.economy.model.SavingsBucket
import ru.finny.petgame.economy.model.SavingsOperationType
import ru.finny.petgame.economy.model.WithdrawResult

class GameRepository(
    private val database: PetDatabase,
    private val engine: EconomyEngine,
    private val now: () -> Long = System::currentTimeMillis,
) {

    private fun profileDao() = database.profileDao()
    private fun balanceDao() = database.balanceDao()
    private fun purchaseDao() = database.purchaseDao()
    private fun savingsDao() = database.savingsDao()
    private fun progressDao() = database.progressDao()
    private fun earningDao() = database.earningDao()
    private fun periodDao() = database.periodDao()
    private fun budgetPlanItemDao() = database.budgetPlanItemDao()
    private fun savingsOperationDao() = database.savingsOperationDao()
    private fun completedTaskDao() = database.completedTaskDao()

    suspend fun createProfile(
        playerName: String,
        petName: String,
        petSpecies: Int,
        petColor: Int,
        playerAvatar: Int = 0,
    ): Long = database.withTransaction {
        val profileId = profileDao().insert(
            ProfileEntity(
                playerName = playerName,
                petName = petName,
                petSpecies = petSpecies,
                petColor = petColor,
                createdAt = now(),
                playerAvatar = playerAvatar,
            ),
        )
        earningDao().insert(
            EarningEntity(
                profileId = profileId,
                source = START_BUDGET_SOURCE,
                amount = START_BUDGET_AMOUNT,
                periodIndex = 0,
                createdAt = now(),
            ),
        )
        balanceDao().upsert(
            BalanceEntity(profileId = profileId, amount = START_BUDGET_AMOUNT, updatedAt = now()),
        )
        profileId
    }

    suspend fun loadSnapshot(): GameSnapshot? = database.withTransaction {
        val profile = profileDao().getCurrentProfile() ?: return@withTransaction null
        val balance = balanceDao().getByProfileId(profile.id)?.amount ?: 0L
        val savings = savingsDao().getByProfileId(profile.id)
        val selectedGoal = progressDao().getByKind(profile.id, PROGRESS_SELECTED_GOAL).firstOrNull()
        val period = periodDao().getLatest(profile.id)
        val plan = period?.let { budgetPlanItemDao().getByPeriod(it.id) }.orEmpty()
        val periodFact = period?.let { openPeriod ->
            val purchases = purchaseDao().getByPeriod(profile.id, openPeriod.periodIndex)
            val deposits = savingsOperationDao().getByPeriod(
                profile.id,
                openPeriod.periodIndex,
                SavingsOperationType.DEPOSIT.name,
            )
            mapOf(
                BudgetDirection.REQUIRED to purchases
                    .filter { row -> row.category == BudgetDirection.REQUIRED.name }
                    .sumOf { row -> row.price },
                BudgetDirection.OPTIONAL to purchases
                    .filter { row -> row.category == BudgetDirection.OPTIONAL.name }
                    .sumOf { row -> row.price },
                BudgetDirection.SAVINGS to deposits.sumOf { row -> row.amount },
            )
        } ?: emptyMap()
        val historyIndex = period?.periodIndex ?: 0
        val periodPurchases = purchaseDao().getByPeriod(profile.id, historyIndex)
        val completedTasks = completedTaskDao().getByProfileId(profile.id)
        val achievedGoalIds = progressDao()
            .getByKind(profile.id, PROGRESS_GOAL_ACHIEVED)
            .map { it.itemId }
            .toSet()
        GameSnapshot(
            profile = profile,
            balance = balance,
            savings = savings,
            savingsTotal = savings.sumOf { it.savedAmount },
            selectedGoalId = selectedGoal?.itemId,
            selectedGoalTitle = selectedGoal?.value,
            currentPeriod = period,
            plan = plan,
            periodFact = periodFact,
            periodPurchases = periodPurchases,
            completedTasks = completedTasks,
            achievedGoalIds = achievedGoalIds,
        )
    }

    suspend fun earn(source: String, amount: Long): EarnResult = database.withTransaction {
        val profile = profileDao().getCurrentProfile() ?: return@withTransaction EarnResult.Error(NO_PROFILE)
        val state = loadEconomyState(profile.id)
        when (val result = engine.earn(state, source, amount)) {
            is EarnResult.Success -> {
                earningDao().insert(
                    EarningEntity(
                        profileId = profile.id,
                        source = result.earning.source,
                        amount = result.earning.amount,
                        periodIndex = currentPeriodIndex(profile.id),
                        createdAt = now(),
                    ),
                )
                balanceDao().upsert(BalanceEntity(profile.id, result.state.balance, now()))
                result
            }
            is EarnResult.Error -> result
        }
    }

    suspend fun saveBudgetPlan(plan: BudgetPlan): PlanCheckResult = database.withTransaction {
        val profile = profileDao().getCurrentProfile()
            ?: return@withTransaction PlanCheckResult.Invalid(listOf(NO_PROFILE))
        val balance = balanceDao().getByProfileId(profile.id)?.amount ?: 0L
        when (val check = engine.checkPlan(plan, balance)) {
            is PlanCheckResult.Invalid -> check
            is PlanCheckResult.Valid -> {
                val period = ensureOpenPeriod(profile.id)
                if (period.status != PeriodStatus.PLANNED.name) {
                    return@withTransaction PlanCheckResult.Invalid(listOf(EXPLANATION_PLAN_CONFIRMED))
                }
                budgetPlanItemDao().deleteByPeriodId(period.id)
                budgetPlanItemDao().upsertAll(
                    plan.amounts.map { (direction, amount) ->
                        BudgetPlanItemEntity(
                            periodId = period.id,
                            direction = direction.name,
                            plannedAmount = amount,
                        )
                    },
                )
                check
            }
        }
    }

    suspend fun confirmBudgetPlan(): Boolean = database.withTransaction {
        val profile = profileDao().getCurrentProfile() ?: return@withTransaction false
        val period = periodDao().getLatest(profile.id) ?: return@withTransaction false
        if (period.status != PeriodStatus.PLANNED.name) return@withTransaction false
        val plan = budgetPlanItemDao().getByPeriod(period.id)
        if (plan.isEmpty()) return@withTransaction false
        periodDao().update(period.copy(status = PeriodStatus.ACTIVE.name))
        true
    }

    suspend fun purchase(draft: PurchaseDraft): PurchaseResult = database.withTransaction {
        val profile = profileDao().getCurrentProfile()
            ?: return@withTransaction PurchaseResult.Invalid(NO_PROFILE)
        val state = loadEconomyState(profile.id)
        when (val result = engine.purchase(state, draft)) {
            is PurchaseResult.Success -> {
                purchaseDao().insert(
                    PurchaseEntity(
                        profileId = profile.id,
                        itemId = result.purchase.itemId,
                        title = result.purchase.title,
                        category = result.purchase.category.name,
                        price = result.purchase.price,
                        periodIndex = currentPeriodIndex(profile.id),
                        purchasedAt = now(),
                    ),
                )
                balanceDao().upsert(BalanceEntity(profile.id, result.state.balance, now()))
                result
            }
            is PurchaseResult.InsufficientFunds -> result
            is PurchaseResult.Invalid -> result
        }
    }

    suspend fun purchaseShopItem(
        itemId: String,
        title: String,
        category: PurchaseCategory,
        price: Long,
        moodDelta: Int,
        satietyDelta: Int,
    ): ShopPurchaseResult = database.withTransaction {
        val profile = profileDao().getCurrentProfile()
            ?: return@withTransaction ShopPurchaseResult.Invalid(NO_PROFILE)
        val state = loadEconomyState(profile.id)
        when (val result = engine.purchase(
            state,
            PurchaseDraft(itemId = itemId, title = title, category = category, price = price),
        )) {
            is PurchaseResult.Success -> {
                purchaseDao().insert(
                    PurchaseEntity(
                        profileId = profile.id,
                        itemId = result.purchase.itemId,
                        title = result.purchase.title,
                        category = result.purchase.category.name,
                        price = result.purchase.price,
                        periodIndex = currentPeriodIndex(profile.id),
                        purchasedAt = now(),
                    ),
                )
                balanceDao().upsert(BalanceEntity(profile.id, result.state.balance, now()))
                val newMood = engine.applyPetEffect(profile.mood, moodDelta)
                val newSaturation = engine.applyPetEffect(profile.saturation, satietyDelta)
                profileDao().update(profile.copy(mood = newMood, saturation = newSaturation))
                ShopPurchaseResult.Success(
                    title = title,
                    price = price,
                    balance = result.state.balance,
                    mood = newMood,
                    saturation = newSaturation,
                    moodDelta = moodDelta,
                    satietyDelta = satietyDelta,
                )
            }
            is PurchaseResult.InsufficientFunds -> ShopPurchaseResult.InsufficientFunds(
                balance = result.balance,
                shortfall = result.shortfall,
                explanation = result.explanation,
            )
            is PurchaseResult.Invalid -> ShopPurchaseResult.Invalid(result.explanation)
        }
    }

    suspend fun selectGoal(goalId: String, goalTitle: String, goalCost: Long): Boolean =
        database.withTransaction {
            val profile = profileDao().getCurrentProfile() ?: return@withTransaction false
            val alreadyAchieved = progressDao()
                .getByKind(profile.id, PROGRESS_GOAL_ACHIEVED)
                .any { it.itemId == goalId }
            if (alreadyAchieved) {
                return@withTransaction false
            }
            val existing = savingsDao().getByGoal(profile.id, goalId)
            val ts = now()
            if (existing == null) {
                savingsDao().upsert(
                    SavingsEntity(
                        profileId = profile.id,
                        goalId = goalId,
                        goalTitle = goalTitle,
                        goalCost = goalCost,
                        savedAmount = 0L,
                        createdAt = ts,
                        updatedAt = ts,
                    ),
                )
            }
            progressDao().deleteByKind(profile.id, PROGRESS_SELECTED_GOAL)
            progressDao().upsert(
                ProgressEntity(
                    profileId = profile.id,
                    kind = PROGRESS_SELECTED_GOAL,
                    itemId = goalId,
                    value = goalTitle,
                    updatedAt = ts,
                ),
            )
            true
        }

    suspend fun saveProgress(kind: String, itemId: String, value: String): Boolean =
        database.withTransaction {
            val profile = profileDao().getCurrentProfile() ?: return@withTransaction false
            progressDao().upsert(
                ProgressEntity(
                    profileId = profile.id,
                    kind = kind,
                    itemId = itemId,
                    value = value,
                    updatedAt = now(),
                ),
            )
            true
        }

    suspend fun depositToSavings(goalId: String, amount: Long): DepositResult = database.withTransaction {
        val profile = profileDao().getCurrentProfile()
            ?: return@withTransaction DepositResult.Invalid(NO_PROFILE)
        val alreadyAchieved = progressDao()
            .getByKind(profile.id, PROGRESS_GOAL_ACHIEVED)
            .any { it.itemId == goalId }
        if (alreadyAchieved) {
            return@withTransaction DepositResult.Invalid(EXPLANATION_GOAL_RECEIVED)
        }
        val state = loadEconomyState(profile.id)
        when (val result = engine.depositToSavings(state, goalId, amount)) {
            is DepositResult.Success -> {
                persistSavingsResult(profile.id, goalId, result.state.balance, result.bucket.savedAmount)
                savingsOperationDao().insert(
                    SavingsOperationEntity(
                        profileId = profile.id,
                        goalId = goalId,
                        type = SavingsOperationType.DEPOSIT.name,
                        amount = amount,
                        periodIndex = currentPeriodIndex(profile.id),
                        createdAt = now(),
                    ),
                )
                result
            }
            is DepositResult.InsufficientFunds -> result
            is DepositResult.Invalid -> result
        }
    }

    suspend fun withdrawFromSavings(goalId: String, amount: Long, confirmed: Boolean): WithdrawResult =
        database.withTransaction {
            val profile = profileDao().getCurrentProfile()
                ?: return@withTransaction WithdrawResult.Invalid(NO_PROFILE)
            val state = loadEconomyState(profile.id)
            val averageDeposit = savingsOperationDao()
                .getDepositsByGoal(profile.id, goalId)
                .map { it.amount }
                .let { engine.averageDeposit(it) }
            when (val result = engine.withdrawFromSavings(state, goalId, amount, confirmed, averageDeposit)) {
                is WithdrawResult.NeedsConfirmation -> result
                is WithdrawResult.Success -> {
                    persistSavingsResult(profile.id, goalId, result.state.balance, result.bucket.savedAmount)
                    savingsOperationDao().insert(
                        SavingsOperationEntity(
                            profileId = profile.id,
                            goalId = goalId,
                            type = SavingsOperationType.WITHDRAW.name,
                            amount = amount,
                            periodIndex = currentPeriodIndex(profile.id),
                            createdAt = now(),
                        ),
                    )
                    result
                }
                is WithdrawResult.Invalid -> result
            }
        }

    suspend fun getGoalEta(goalId: String): GoalEta? {
        val profile = profileDao().getCurrentProfile() ?: return null
        val row = savingsDao().getByGoal(profile.id, goalId) ?: return null
        val deposits = savingsOperationDao().getDepositsByGoal(profile.id, goalId).map { it.amount }
        return engine.goalEta(
            SavingsBucket(
                goalId = row.goalId,
                goalTitle = row.goalTitle,
                goalCost = row.goalCost,
                savedAmount = row.savedAmount,
            ),
            engine.averageDeposit(deposits),
        )
    }

    suspend fun achieveGoal(goalId: String): GoalAchieveResult = database.withTransaction {
        val profile = profileDao().getCurrentProfile()
            ?: return@withTransaction GoalAchieveResult.Invalid(NO_PROFILE)
        val alreadyAchieved = progressDao()
            .getByKind(profile.id, PROGRESS_GOAL_ACHIEVED)
            .any { it.itemId == goalId }
        if (alreadyAchieved) {
            return@withTransaction GoalAchieveResult.Invalid(EXPLANATION_GOAL_RECEIVED)
        }
        val row = savingsDao().getByGoal(profile.id, goalId)
            ?: return@withTransaction GoalAchieveResult.Invalid(EXPLANATION_GOAL_REQUIRED)
        if (row.savedAmount < row.goalCost) {
            return@withTransaction GoalAchieveResult.Invalid(
                "Пока не хватает монет. Осталось накопить: ${row.goalCost - row.savedAmount}.",
            )
        }
        val newSaved = row.savedAmount - row.goalCost
        savingsDao().update(row.copy(savedAmount = newSaved, updatedAt = now()))
        progressDao().upsert(
            ProgressEntity(
                profileId = profile.id,
                kind = PROGRESS_GOAL_ACHIEVED,
                itemId = goalId,
                value = row.goalTitle,
                updatedAt = now(),
            ),
        )
        val newMood = engine.applyPetEffect(profile.mood, GOAL_MOOD_REWARD)
        profileDao().update(profile.copy(mood = newMood))
        progressDao().deleteByKind(profile.id, PROGRESS_SELECTED_GOAL)
        GoalAchieveResult.Success(
            goalTitle = row.goalTitle,
            moodDelta = GOAL_MOOD_REWARD,
            mood = newMood,
            savedLeft = newSaved,
        )
    }

    suspend fun setAllTasksOpen(open: Boolean): Boolean = database.withTransaction {
        val profile = profileDao().getCurrentProfile() ?: return@withTransaction false
        profileDao().update(profile.copy(allTasksOpen = open))
        true
    }

    fun checkTaskDistribution(
        amounts: Map<BudgetDirection, Long>,
        sum: Long,
        minimums: Map<BudgetDirection, Long>,
    ): DistributionCheck = engine.checkDistribution(amounts, sum, minimums)

    suspend fun completeTask(
        taskId: String,
        taskTitle: String,
        theme: String,
        isCorrect: Boolean,
        reward: Long,
    ): TaskCompletionResult = database.withTransaction {
        val profile = profileDao().getCurrentProfile()
            ?: return@withTransaction TaskCompletionResult.Invalid(NO_PROFILE)
        val rewardedAlready = completedTaskDao().getByProfileId(profile.id)
            .any { record -> record.taskId == taskId && record.isCorrect == true }
        if (rewardedAlready) {
            return@withTransaction TaskCompletionResult.AlreadyCompleted
        }
        val grantedReward = if (isCorrect) reward else 0L
        val moodDelta = if (isCorrect) TASK_MOOD_REWARD else 0
        val periodIndex = currentPeriodIndex(profile.id)
        completedTaskDao().insert(
            CompletedTaskEntity(
                profileId = profile.id,
                taskId = taskId,
                theme = theme,
                isCorrect = isCorrect,
                reward = grantedReward,
                periodIndex = periodIndex,
                completedAt = now(),
            ),
        )
        val state = loadEconomyState(profile.id)
        var newBalance = state.balance
        if (isCorrect && grantedReward > 0L) {
            val earningSource = "Задание: $taskTitle"
            when (val earnResult = engine.earn(state, earningSource, grantedReward)) {
                is EarnResult.Success -> {
                    earningDao().insert(
                        EarningEntity(
                            profileId = profile.id,
                            source = earningSource,
                            amount = earnResult.earning.amount,
                            periodIndex = periodIndex,
                            createdAt = now(),
                        ),
                    )
                    balanceDao().upsert(BalanceEntity(profile.id, earnResult.state.balance, now()))
                    newBalance = earnResult.state.balance
                }
                is EarnResult.Error -> {}
            }
        }
        val newMood = engine.applyPetEffect(profile.mood, moodDelta)
        profileDao().update(profile.copy(mood = newMood))
        TaskCompletionResult.Success(
            isCorrect = isCorrect,
            reward = grantedReward,
            balance = newBalance,
            mood = newMood,
            moodDelta = moodDelta,
        )
    }

    suspend fun closePeriod(): PeriodCloseResult = database.withTransaction {
        val profile = profileDao().getCurrentProfile()
            ?: return@withTransaction PeriodCloseResult.Invalid(NO_PROFILE)
        val period = periodDao().getLatest(profile.id)
            ?: return@withTransaction PeriodCloseResult.Invalid(EXPLANATION_NO_PERIOD)
        if (period.status != PeriodStatus.ACTIVE.name) {
            return@withTransaction PeriodCloseResult.Invalid(EXPLANATION_PERIOD_NOT_ACTIVE)
        }
        val planItems = budgetPlanItemDao().getByPeriod(period.id)
        if (planItems.isEmpty()) {
            return@withTransaction PeriodCloseResult.Invalid(EXPLANATION_NO_PLAN)
        }
        val plan = planItems.associate { BudgetDirection.valueOf(it.direction) to it.plannedAmount }
        val purchases = purchaseDao().getByPeriod(profile.id, period.periodIndex)
        val deposits = savingsOperationDao().getByPeriod(
            profile.id,
            period.periodIndex,
            SavingsOperationType.DEPOSIT.name,
        )
        val fact = mapOf(
            BudgetDirection.REQUIRED to purchases
                .filter { row -> row.category == PurchaseCategory.REQUIRED.name }
                .sumOf { row -> row.price },
            BudgetDirection.OPTIONAL to purchases
                .filter { row -> row.category == PurchaseCategory.OPTIONAL.name }
                .sumOf { row -> row.price },
            BudgetDirection.SAVINGS to deposits.sumOf { row -> row.amount },
        )
        val review = engine.evaluatePeriod(plan, fact)
        val previousMood = profile.mood
        val previousStage = profile.petStage.coerceIn(0, EconomyEngine.MAX_PET_STAGE)
        val newMood = engine.applyPetEffect(previousMood, review.moodDelta)
        val newStage = engine.nextPetStage(previousStage, review.isGoodPeriod)
        val newGoodPeriods = if (review.isGoodPeriod) profile.goodPeriods + 1 else profile.goodPeriods
        profileDao().update(
            profile.copy(
                mood = newMood,
                petStage = newStage,
                goodPeriods = newGoodPeriods,
            ),
        )
        periodDao().update(period.copy(status = PeriodStatus.CLOSED.name, closedAt = now()))
        ensureOpenPeriod(profile.id)

        var newBalance = balanceDao().getByProfileId(profile.id)?.amount ?: 0L
        val periodIncome = EconomyEngine.PERIOD_INCOME_AMOUNT
        when (
            val earnResult = engine.earn(
                loadEconomyState(profile.id),
                EconomyEngine.PERIOD_INCOME_SOURCE,
                periodIncome,
            )
        ) {
            is EarnResult.Success -> {
                earningDao().insert(
                    EarningEntity(
                        profileId = profile.id,
                        source = earnResult.earning.source,
                        amount = earnResult.earning.amount,
                        periodIndex = currentPeriodIndex(profile.id),
                        createdAt = now(),
                    ),
                )
                balanceDao().upsert(BalanceEntity(profile.id, earnResult.state.balance, now()))
                newBalance = earnResult.state.balance
            }
            is EarnResult.Error -> {}
        }

        PeriodCloseResult.Success(
            periodIndex = period.periodIndex,
            review = review,
            previousMood = previousMood,
            newMood = newMood,
            previousStage = previousStage,
            newStage = newStage,
            stageGrew = newStage > previousStage,
            periodIncome = periodIncome,
            newBalance = newBalance,
        )
    }

    fun evaluateCurrentPeriod(snapshot: GameSnapshot): PeriodReview? {
        val period = snapshot.currentPeriod ?: return null
        if (period.status != PeriodStatus.ACTIVE.name) return null
        if (snapshot.plan.isEmpty()) return null
        val plan = snapshot.plan.associate { BudgetDirection.valueOf(it.direction) to it.plannedAmount }
        return engine.evaluatePeriod(plan, snapshot.periodFact)
    }

    private suspend fun persistSavingsResult(profileId: Long, goalId: String, newBalance: Long, newSaved: Long) {
        val row = savingsDao().getByGoal(profileId, goalId) ?: return
        savingsDao().update(row.copy(savedAmount = newSaved, updatedAt = now()))
        balanceDao().upsert(BalanceEntity(profileId = profileId, amount = newBalance, updatedAt = now()))
    }

    private suspend fun loadEconomyState(profileId: Long): EconomyState {
        val balance = balanceDao().getByProfileId(profileId)?.amount ?: 0L
        val buckets = savingsDao().getByProfileId(profileId).map { row ->
            SavingsBucket(
                goalId = row.goalId,
                goalTitle = row.goalTitle,
                goalCost = row.goalCost,
                savedAmount = row.savedAmount,
            )
        }
        return EconomyState(
            balance = balance,
            savings = buckets.associateBy { it.goalId },
        )
    }

    private suspend fun ensureOpenPeriod(profileId: Long): PeriodEntity {
        val latest = periodDao().getLatest(profileId)
        if (latest != null && latest.status != PeriodStatus.CLOSED.name) {
            return latest
        }
        val nextIndex = (latest?.periodIndex ?: -1) + 1
        periodDao().insert(
            PeriodEntity(
                profileId = profileId,
                periodIndex = nextIndex,
                status = PeriodStatus.PLANNED.name,
                createdAt = now(),
                closedAt = null,
            ),
        )
        return periodDao().getByIndex(profileId, nextIndex)!!
    }

    private suspend fun currentPeriodIndex(profileId: Long): Int {
        val latest = periodDao().getLatest(profileId) ?: return 0
        return if (latest.status == PeriodStatus.CLOSED.name) {
            latest.periodIndex + 1
        } else {
            latest.periodIndex
        }
    }

    companion object {
        const val PROGRESS_SELECTED_GOAL = "SELECTED_GOAL"
        const val PROGRESS_GOAL_ACHIEVED = "GOAL_ACHIEVED"
        const val START_BUDGET_SOURCE = "START_BUDGET"
        const val START_BUDGET_AMOUNT = 30L
        private const val GOAL_MOOD_REWARD = 15
        private const val TASK_MOOD_REWARD = 10
        private const val NO_PROFILE = "Сначала создай профиль."
        private const val EXPLANATION_GOAL_REQUIRED = "Сначала выбери цель накопления."
        private const val EXPLANATION_GOAL_RECEIVED = "Эта цель уже получена. Выбери новую цель."
        private const val EXPLANATION_PLAN_CONFIRMED = "План уже подтверждён. Его нельзя менять в этом периоде."
        private const val EXPLANATION_NO_PERIOD = "Сначала составь план периода."
        private const val EXPLANATION_PERIOD_NOT_ACTIVE =
            "Сначала подтверди план. Завершить период можно после этого."
        private const val EXPLANATION_NO_PLAN = "Сначала составь и подтверди план."
    }
}
