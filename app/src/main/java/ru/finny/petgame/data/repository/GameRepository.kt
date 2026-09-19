package ru.finny.petgame.data.repository

import androidx.room.withTransaction
import ru.finny.petgame.data.PetDatabase
import ru.finny.petgame.data.entity.BalanceEntity
import ru.finny.petgame.data.entity.BudgetPlanItemEntity
import ru.finny.petgame.data.entity.EarningEntity
import ru.finny.petgame.data.entity.PeriodEntity
import ru.finny.petgame.data.entity.ProfileEntity
import ru.finny.petgame.data.entity.ProgressEntity
import ru.finny.petgame.data.entity.PurchaseEntity
import ru.finny.petgame.data.entity.SavingsEntity
import ru.finny.petgame.data.entity.SavingsOperationEntity
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.data.model.PeriodStatus
import ru.finny.petgame.data.model.ShopPurchaseResult
import ru.finny.petgame.economy.EconomyEngine
import ru.finny.petgame.economy.EconomyState
import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.economy.model.BudgetPlan
import ru.finny.petgame.economy.model.DepositResult
import ru.finny.petgame.economy.model.EarnResult
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

    suspend fun closePeriod(): Boolean = database.withTransaction {
        val profile = profileDao().getCurrentProfile() ?: return@withTransaction false
        val period = periodDao().getLatest(profile.id) ?: return@withTransaction false
        if (period.status == PeriodStatus.CLOSED.name) return@withTransaction false
        periodDao().update(period.copy(status = PeriodStatus.CLOSED.name, closedAt = now()))
        true
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
        const val START_BUDGET_SOURCE = "START_BUDGET"
        const val START_BUDGET_AMOUNT = 30L
        private const val NO_PROFILE = "Сначала создай профиль."
        private const val EXPLANATION_PLAN_CONFIRMED =
            "План уже подтверждён. Изменить его можно в новом периоде."
    }
}