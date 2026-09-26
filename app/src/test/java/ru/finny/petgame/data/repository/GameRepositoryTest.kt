package ru.finny.petgame.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.finny.petgame.data.PetDatabase
import ru.finny.petgame.data.entity.ProfileEntity
import ru.finny.petgame.data.model.GoalAchieveResult
import ru.finny.petgame.data.model.LedgerKind
import ru.finny.petgame.data.model.PeriodCloseResult
import ru.finny.petgame.data.model.PeriodStatus
import ru.finny.petgame.data.model.ShopPurchaseResult
import ru.finny.petgame.data.model.TaskCompletionResult
import ru.finny.petgame.economy.EconomyEngine
import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.economy.model.BudgetPlan
import ru.finny.petgame.economy.model.DepositResult
import ru.finny.petgame.economy.model.EarnResult
import ru.finny.petgame.economy.model.PlanCheckResult
import ru.finny.petgame.economy.model.PurchaseCategory
import ru.finny.petgame.economy.model.PurchaseDraft
import ru.finny.petgame.economy.model.PurchaseResult
import ru.finny.petgame.economy.model.SavingsOperationType
import ru.finny.petgame.economy.model.WeekNeed
import ru.finny.petgame.economy.model.WithdrawResult

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameRepositoryTest {

    private val fixedTime = 1_700_000_000_000L
    private lateinit var db: PetDatabase
    private lateinit var repository: GameRepository
    private var profileId: Long = 0L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PetDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = GameRepository(db, EconomyEngine()) { fixedTime }
        profileId = repository.createProfile(
            playerName = "Аня",
            petName = "Финни",
            petHat = 1,
            petFace = 0,
            petOutfit = 2,
            petEmotion = 0,
            petEyeColor = 1,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun freshRepository(): GameRepository = GameRepository(db, EconomyEngine()) { fixedTime }

    private fun plan(required: Long, optional: Long, savings: Long) = BudgetPlan(
        mapOf(
            BudgetDirection.REQUIRED to required,
            BudgetDirection.OPTIONAL to optional,
            BudgetDirection.SAVINGS to savings,
        ),
    )

    @Test
    fun `profile with start budget is read back by fresh repository`() = runTest {
        val snapshot = freshRepository().loadSnapshot()

        assertTrue(snapshot != null)
        snapshot!!
        assertEquals("Аня", snapshot.profile.playerName)
        assertEquals("Финни", snapshot.profile.petName)
        assertEquals(1, snapshot.profile.petHat)
        assertEquals(0, snapshot.profile.petFace)
        assertEquals(2, snapshot.profile.petOutfit)
        assertEquals(0, snapshot.profile.petEmotion)
        assertEquals(1, snapshot.profile.petEyeColor)
        assertEquals(GameRepository.START_BUDGET_AMOUNT, snapshot.balance)
        assertEquals(0L, snapshot.savingsTotal)
        assertEquals(70, snapshot.profile.mood)
        assertEquals(70, snapshot.profile.saturation)
        assertNull(snapshot.selectedGoalId)
        assertNull(snapshot.currentPeriod)

        val earnings = db.earningDao().getByPeriod(profileId, 0)
        assertEquals(1, earnings.size)
        assertEquals(GameRepository.START_BUDGET_SOURCE, earnings[0].source)
        assertEquals(GameRepository.START_BUDGET_AMOUNT, earnings[0].amount)
    }

    @Test
    fun `wardrobe updates look and name`() = runTest {
        val saved = repository.updatePetAppearance(
            petName = "Лиса",
            petHat = 2,
            petFace = 1,
            petOutfit = 0,
            petEmotion = 3,
            petEyeColor = 2,
        )
        assertTrue(saved)

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals("Лиса", snapshot.profile.petName)
        assertEquals(2, snapshot.profile.petHat)
        assertEquals(1, snapshot.profile.petFace)
        assertEquals(0, snapshot.profile.petOutfit)
        assertEquals(3, snapshot.profile.petEmotion)
        assertEquals(2, snapshot.profile.petEyeColor)
        assertEquals(GameRepository.START_BUDGET_AMOUNT, snapshot.balance)
    }

    @Test
    fun `earn persists balance and earning record`() = runTest {
        val result = repository.earn(source = "TASK", amount = 50L)
        assertTrue(result is EarnResult.Success)

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(GameRepository.START_BUDGET_AMOUNT + 50L, snapshot.balance)

        val earnings = db.earningDao().getByPeriod(profileId, 0)
        assertEquals(2, earnings.size)
        assertTrue(earnings.any { it.source == "TASK" && it.amount == 50L })
    }

    @Test
    fun `purchase persists row with category and reduces balance`() = runTest {
        repository.earn(source = "TASK", amount = 100L)
        val result = repository.purchase(
            PurchaseDraft("food_1", "Корм", PurchaseCategory.REQUIRED, 30L),
        )
        assertTrue(result is PurchaseResult.Success)

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(GameRepository.START_BUDGET_AMOUNT + 100L - 30L, snapshot.balance)

        val purchases = db.purchaseDao().getByPeriod(profileId, 0)
        assertEquals(1, purchases.size)
        assertEquals("food_1", purchases[0].itemId)
        assertEquals(PurchaseCategory.REQUIRED.name, purchases[0].category)
        assertEquals(30L, purchases[0].price)
    }

    @Test
    fun `insufficient purchase changes nothing`() = runTest {
        repository.earn(source = "TASK", amount = 10L)
        val result = repository.purchase(
            PurchaseDraft("toy_1", "Мяч", PurchaseCategory.OPTIONAL, 60L),
        )
        assertTrue(result is PurchaseResult.InsufficientFunds)
        assertEquals(20L, (result as PurchaseResult.InsufficientFunds).shortfall)

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(GameRepository.START_BUDGET_AMOUNT + 10L, snapshot.balance)
        assertTrue(db.purchaseDao().getByPeriod(profileId, 0).isEmpty())
    }

    @Test
    fun `budget plan is saved and persists`() = runTest {
        repository.earn(source = "TASK", amount = 100L)
        val result = repository.saveBudgetPlan(plan(40, 20, 25))
        assertTrue(result is PlanCheckResult.Valid)
        assertEquals(45L, (result as PlanCheckResult.Valid).remainder)

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(PeriodStatus.PLANNED.name, snapshot.currentPeriod?.status)
        assertEquals(0, snapshot.currentPeriod?.periodIndex)
        assertEquals(3, snapshot.plan.size)
        assertTrue(snapshot.plan.any { it.direction == BudgetDirection.REQUIRED.name && it.plannedAmount == 40L })
        assertTrue(snapshot.plan.any { it.direction == BudgetDirection.OPTIONAL.name && it.plannedAmount == 20L })
        assertTrue(snapshot.plan.any { it.direction == BudgetDirection.SAVINGS.name && it.plannedAmount == 25L })
    }

    @Test
    fun `plan above balance is rejected and not saved`() = runTest {
        repository.earn(source = "TASK", amount = 50L)
        val result = repository.saveBudgetPlan(plan(40, 40, 40))
        assertTrue(result is PlanCheckResult.Invalid)

        val snapshot = freshRepository().loadSnapshot()!!
        assertNull(snapshot.currentPeriod)
        assertTrue(snapshot.plan.isEmpty())
    }

    @Test
    fun `plan cannot be changed after confirmation`() = runTest {
        repository.earn(source = "TASK", amount = 100L)
        repository.saveBudgetPlan(plan(40, 20, 25))
        assertTrue(repository.confirmBudgetPlan())

        val second = repository.saveBudgetPlan(plan(50, 10, 20))
        assertTrue(second is PlanCheckResult.Invalid)

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(PeriodStatus.ACTIVE.name, snapshot.currentPeriod?.status)
        assertEquals(40L, snapshot.plan.first { it.direction == BudgetDirection.REQUIRED.name }.plannedAmount)
    }

    @Test
    fun `savings eta uses average deposit per week`() = runTest {
        repository.earn(source = "TASK", amount = 100L)
        assertTrue(repository.selectGoal("goal_1", "Велосипед", 200L))
        repository.depositToSavings("goal_1", 30L)
        repository.depositToSavings("goal_1", 20L)

        val eta = repository.getGoalEta("goal_1")

        assertTrue(eta != null)
        eta!!
        assertEquals(50L, eta.averageDeposit)
        assertEquals(3, eta.periodsLeft)
        assertEquals(50L, freshRepository().loadSnapshot()!!.savingsTotal)
    }

    @Test
    fun `goal achievement reduces savings raises mood and marks progress`() = runTest {
        repository.earn(source = "TASK", amount = 200L)
        assertTrue(repository.selectGoal("goal_1", "Велосипед", 200L))
        repository.depositToSavings("goal_1", 200L)

        val result = repository.achieveGoal("goal_1")

        assertTrue(result is GoalAchieveResult.Success)
        val success = result as GoalAchieveResult.Success
        assertEquals(15, success.moodDelta)
        assertEquals(85, success.mood)
        assertEquals(0L, success.savedLeft)

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(85, snapshot.profile.mood)
        assertEquals(0L, snapshot.savings.first { it.goalId == "goal_1" }.savedAmount)
        val progress = db.progressDao().getByKind(profileId, GameRepository.PROGRESS_GOAL_ACHIEVED)
        assertEquals(1, progress.size)
        assertEquals("goal_1", progress.first().itemId)
        assertEquals(2, snapshot.profile.petOutfit)
    }

    @Test
    fun `receiving the festive outfit dresses the pet`() = runTest {
        repository.earn(source = "TASK", amount = 450L)
        assertTrue(repository.selectGoal(GameRepository.GOAL_OUTFIT_ID, "Праздничный наряд", 450L))
        repository.depositToSavings(GameRepository.GOAL_OUTFIT_ID, 450L)

        val result = repository.achieveGoal(GameRepository.GOAL_OUTFIT_ID)

        assertTrue(result is GoalAchieveResult.Success)
        assertEquals(
            ProfileEntity.FESTIVE_OUTFIT,
            freshRepository().loadSnapshot()!!.profile.petOutfit,
        )
    }

    @Test
    fun `goal cannot be achieved before enough saved`() = runTest {
        repository.earn(source = "TASK", amount = 100L)
        assertTrue(repository.selectGoal("goal_1", "Велосипед", 200L))
        repository.depositToSavings("goal_1", 50L)

        val result = repository.achieveGoal("goal_1")

        assertTrue(result is GoalAchieveResult.Invalid)
        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(50L, snapshot.savings.first { it.goalId == "goal_1" }.savedAmount)
        assertEquals(70, snapshot.profile.mood)
    }

    @Test
    fun `task reward is granted once`() = runTest {
        val first = repository.completeTask(
            taskId = "task_budget_1",
            taskTitle = "Первые монеты",
            theme = "BUDGET",
            isCorrect = true,
            reward = 5L,
        )
        assertTrue(first is TaskCompletionResult.Success)
        val success = first as TaskCompletionResult.Success
        assertEquals(true, success.isCorrect)
        assertEquals(5L, success.reward)
        assertEquals(GameRepository.START_BUDGET_AMOUNT + 5L, success.balance)
        assertEquals(75, success.mood)
        assertTrue(success.firstTry)

        val second = repository.completeTask(
            taskId = "task_budget_1",
            taskTitle = "Первые монеты",
            theme = "BUDGET",
            isCorrect = true,
            reward = 5L,
        )
        assertTrue(second is TaskCompletionResult.AlreadyCompleted)
        assertEquals(GameRepository.START_BUDGET_AMOUNT + 5L, freshRepository().loadSnapshot()!!.balance)

        val earnings = db.earningDao()
            .getByPeriod(profileId, 0)
            .filter { it.source == "Задание: Первые монеты" }
        assertEquals(1, earnings.size)
        assertEquals(5L, earnings.first().amount)
    }

    @Test
    fun `wrong task answer does not change balance`() = runTest {
        val result = repository.completeTask(
            taskId = "t1",
            taskTitle = "Задание",
            theme = "BUDGET",
            isCorrect = false,
            reward = 10L,
        )
        assertTrue(result is TaskCompletionResult.Success)
        val success = result as TaskCompletionResult.Success
        assertEquals(false, success.isCorrect)
        assertEquals(0L, success.reward)
        assertEquals(GameRepository.START_BUDGET_AMOUNT, success.balance)
        assertEquals(70, success.mood)

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(1, snapshot.completedTasks.size)
        val record = snapshot.completedTasks.first()
        assertEquals("t1", record.taskId)
        assertEquals(false, record.isCorrect)
        assertEquals(0L, record.reward)
        assertEquals("BUDGET", record.theme)
    }

    @Test
    fun `completed tasks persist between launches`() = runTest {
        repository.completeTask("task_budget_1", "Первые монеты", "BUDGET", true, 5L)
        repository.completeTask("task_savings_1", "Домик", "SAVINGS", false, 5L)

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(2, snapshot.completedTasks.size)
        val rewarded = snapshot.completedTasks.first { it.taskId == "task_budget_1" }
        assertEquals(true, rewarded.isCorrect)
        assertEquals(5L, rewarded.reward)
        assertEquals("BUDGET", rewarded.theme)
        val wrong = snapshot.completedTasks.first { it.taskId == "task_savings_1" }
        assertEquals(false, wrong.isCorrect)
        assertEquals(0L, wrong.reward)
    }

    @Test
    fun `achieved goal cannot be selected or saved again`() = runTest {
        repository.earn(source = "TASK", amount = 200L)
        assertTrue(repository.selectGoal("goal_1", "Велосипед", 200L))
        repository.depositToSavings("goal_1", 200L)
        assertTrue(repository.achieveGoal("goal_1") is GoalAchieveResult.Success)

        assertFalse(repository.selectGoal("goal_1", "Велосипед", 200L))
        val deposit = repository.depositToSavings("goal_1", 10L)
        assertTrue(deposit is DepositResult.Invalid)
        assertTrue(repository.achieveGoal("goal_1") is GoalAchieveResult.Invalid)

        val snapshot = freshRepository().loadSnapshot()!!
        assertNull(snapshot.selectedGoalId)
        assertTrue(snapshot.achievedGoalIds.contains("goal_1"))
        assertEquals(GameRepository.START_BUDGET_AMOUNT + 200L - 200L, snapshot.balance)
    }

    @Test
    fun `withdrawal preview and confirmed withdraw persist`() = runTest {
        repository.earn(source = "TASK", amount = 100L)
        assertTrue(repository.selectGoal("goal_1", "Велосипед", 200L))
        repository.depositToSavings("goal_1", 80L)

        val preview = repository.withdrawFromSavings("goal_1", 30L, confirmed = false)
        assertTrue(preview is WithdrawResult.NeedsConfirmation)
        val previewResult = preview as WithdrawResult.NeedsConfirmation
        assertEquals(50L, previewResult.newSavedAmount)
        assertEquals(2, previewResult.eta.periodsLeft)

        val confirmed = repository.withdrawFromSavings("goal_1", 30L, confirmed = true)
        assertTrue(confirmed is WithdrawResult.Success)

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(GameRepository.START_BUDGET_AMOUNT + 100L - 80L + 30L, snapshot.balance)
        assertEquals(50L, snapshot.savings.first().savedAmount)
        assertEquals(
            1,
            db.savingsOperationDao().getByPeriod(profileId, 0, SavingsOperationType.WITHDRAW.name).size,
        )
    }

    @Test
    fun `period fact is computed from purchases and savings deposits`() = runTest {
        repository.earn(source = "TASK", amount = 100L)
        repository.saveBudgetPlan(plan(40, 20, 25))
        assertTrue(repository.confirmBudgetPlan())
        repository.selectGoal("goal_1", "Велосипед", 200L)
        repository.purchase(PurchaseDraft("food_1", "Корм", PurchaseCategory.REQUIRED, 10L))
        repository.purchase(PurchaseDraft("toy_1", "Мяч", PurchaseCategory.OPTIONAL, 5L))
        repository.depositToSavings("goal_1", 7L)

        val snapshot = freshRepository().loadSnapshot()!!

        assertEquals(10L, snapshot.periodFact[BudgetDirection.REQUIRED])
        assertEquals(5L, snapshot.periodFact[BudgetDirection.OPTIONAL])
        assertEquals(7L, snapshot.periodFact[BudgetDirection.SAVINGS])
    }

    @Test
    fun `shop purchase subtracts price and raises mood and saturation`() = runTest {
        val result = repository.purchaseShopItem(
            itemId = "food_basic",
            title = "Корм",
            category = PurchaseCategory.REQUIRED,
            price = 10L,
            moodDelta = 10,
            satietyDelta = 15,
        )
        assertTrue(result is ShopPurchaseResult.Success)
        val success = result as ShopPurchaseResult.Success
        assertEquals(GameRepository.START_BUDGET_AMOUNT - 10L, success.balance)
        assertEquals(80, success.mood)
        assertEquals(85, success.saturation)

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(GameRepository.START_BUDGET_AMOUNT - 10L, snapshot.balance)
        assertEquals(80, snapshot.profile.mood)
        assertEquals(85, snapshot.profile.saturation)
        assertEquals(1, snapshot.periodPurchases.size)
        assertEquals(10L, snapshot.periodPurchases.first().price)
    }

    @Test
    fun `shop purchase without funds keeps balance and returns shortfall`() = runTest {
        repository.earn(source = "TASK", amount = 10L)
        val result = repository.purchaseShopItem(
            itemId = "toy_1",
            title = "Мяч",
            category = PurchaseCategory.OPTIONAL,
            price = 60L,
            moodDelta = 8,
            satietyDelta = 0,
        )
        assertTrue(result is ShopPurchaseResult.InsufficientFunds)
        val failed = result as ShopPurchaseResult.InsufficientFunds
        assertEquals(20L, failed.shortfall)
        assertTrue(failed.explanation.contains("20"))

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(GameRepository.START_BUDGET_AMOUNT + 10L, snapshot.balance)
        assertEquals(70, snapshot.profile.mood)
        assertEquals(0, snapshot.periodPurchases.size)
    }

    @Test
    fun `shop purchases count in plan fact by category`() = runTest {
        repository.earn(source = "TASK", amount = 100L)
        repository.saveBudgetPlan(plan(40, 20, 25))
        assertTrue(repository.confirmBudgetPlan())
        repository.purchaseShopItem(
            itemId = "food_basic",
            title = "Корм",
            category = PurchaseCategory.REQUIRED,
            price = 10L,
            moodDelta = 10,
            satietyDelta = 15,
        )
        repository.purchaseShopItem(
            itemId = "ball",
            title = "Мяч",
            category = PurchaseCategory.OPTIONAL,
            price = 18L,
            moodDelta = 8,
            satietyDelta = 0,
        )

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(10L, snapshot.periodFact[BudgetDirection.REQUIRED])
        assertEquals(18L, snapshot.periodFact[BudgetDirection.OPTIONAL])
        assertEquals(2, snapshot.periodPurchases.size)
    }

    @Test
    fun `selected goal persists`() = runTest {
        assertTrue(repository.selectGoal("goal_1", "Кормушка", 120L))

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals("goal_1", snapshot.selectedGoalId)
        val goal = snapshot.savings.first()
        assertEquals("goal_1", goal.goalId)
        assertEquals(120L, goal.goalCost)
        assertEquals(0L, goal.savedAmount)
    }

    @Test
    fun `progress persists`() = runTest {
        assertTrue(repository.saveProgress(kind = "PET_STAGE", itemId = "stage", value = "2"))

        val records = db.progressDao().getByKind(profileId, "PET_STAGE")
        assertEquals(1, records.size)
        assertEquals("2", records[0].value)
    }

    @Test
    fun `deposit persists savings balance and operation`() = runTest {
        repository.earn(source = "TASK", amount = 100L)
        repository.selectGoal("goal_1", "Велосипед", 200L)
        val result = repository.depositToSavings("goal_1", 30L)
        assertTrue(result is DepositResult.Success)

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(GameRepository.START_BUDGET_AMOUNT + 100L - 30L, snapshot.balance)
        assertEquals(30L, snapshot.savings.first { it.goalId == "goal_1" }.savedAmount)
        assertEquals("goal_1", snapshot.selectedGoalId)

        val deposits = db.savingsOperationDao().getByPeriod(profileId, 0, SavingsOperationType.DEPOSIT.name)
        assertEquals(1, deposits.size)
        assertEquals(30L, deposits[0].amount)
    }

    @Test
    fun `deposit without funds changes nothing`() = runTest {
        repository.earn(source = "TASK", amount = 10L)
        repository.selectGoal("goal_1", "Велосипед", 200L)
        val result = repository.depositToSavings("goal_1", 50L)
        assertTrue(result is DepositResult.InsufficientFunds)

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(GameRepository.START_BUDGET_AMOUNT + 10L, snapshot.balance)
        assertEquals(0L, snapshot.savings.first().savedAmount)
        assertTrue(
            db.savingsOperationDao().getByPeriod(profileId, 0, SavingsOperationType.DEPOSIT.name).isEmpty(),
        )
    }

    @Test
    fun `withdraw preview is not persisted`() = runTest {
        repository.earn(source = "TASK", amount = 100L)
        repository.selectGoal("goal_1", "Велосипед", 200L)
        repository.depositToSavings("goal_1", 80L)

        val result = repository.withdrawFromSavings("goal_1", 30L, confirmed = false)
        assertTrue(result is WithdrawResult.NeedsConfirmation)
        val preview = result as WithdrawResult.NeedsConfirmation
        assertEquals(50L, preview.newSavedAmount)
        assertEquals(2, preview.eta.periodsLeft)

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(GameRepository.START_BUDGET_AMOUNT + 100L - 80L, snapshot.balance)
        assertEquals(80L, snapshot.savings.first().savedAmount)
        assertTrue(
            db.savingsOperationDao().getByPeriod(profileId, 0, SavingsOperationType.WITHDRAW.name).isEmpty(),
        )
    }

    @Test
    fun `confirmed withdraw persists and writes operation`() = runTest {
        repository.earn(source = "TASK", amount = 100L)
        repository.selectGoal("goal_1", "Велосипед", 200L)
        repository.depositToSavings("goal_1", 80L)

        val result = repository.withdrawFromSavings("goal_1", 30L, confirmed = true)
        assertTrue(result is WithdrawResult.Success)

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(GameRepository.START_BUDGET_AMOUNT + 100L - 80L + 30L, snapshot.balance)
        assertEquals(50L, snapshot.savings.first().savedAmount)

        val withdrawals = db.savingsOperationDao().getByPeriod(profileId, 0, SavingsOperationType.WITHDRAW.name)
        assertEquals(1, withdrawals.size)
        assertEquals(30L, withdrawals[0].amount)
    }

    @Test
    fun `closePeriod closes current and next operations use next index`() = runTest {
        repository.earn(source = "TASK", amount = 100L)
        repository.saveBudgetPlan(plan(40, 20, 25))
        assertTrue(repository.confirmBudgetPlan())
        repository.purchase(PurchaseDraft("food_1", "Корм", PurchaseCategory.REQUIRED, 40L))
        repository.purchase(PurchaseDraft("toy_1", "Мяч", PurchaseCategory.OPTIONAL, 10L))
        assertTrue(repository.selectGoal("goal_1", "Велосипед", 200L))
        repository.depositToSavings("goal_1", 25L)

        val closed = repository.closePeriod()
        assertTrue(closed is PeriodCloseResult.Success)
        val success = closed as PeriodCloseResult.Success
        assertTrue(success.review.isGoodPeriod)
        assertEquals(3, success.starsEarned)
        assertEquals(3, success.totalStars)
        assertEquals(1, success.starsToNextStage)
        assertEquals(0, success.newStage)
        assertEquals(EconomyEngine.PERIOD_INCOME_AMOUNT, success.periodIncome)
        assertEquals(EconomyEngine.PLAN_BONUS_AMOUNT, success.planBonus)
        assertEquals(70, success.previousSatiety)
        assertEquals(40, success.newSatiety)

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(PeriodStatus.PLANNED.name, snapshot.currentPeriod?.status)
        assertEquals(1, snapshot.currentPeriod?.periodIndex)
        assertEquals(0, snapshot.profile.petStage)
        assertEquals(3, snapshot.profile.growthStars)
        assertEquals(40, snapshot.profile.saturation)
        assertEquals(1, snapshot.profile.goodPeriods)
        assertEquals(1, snapshot.closedPeriodCount)
        assertTrue(snapshot.lastClosedPeriod != null)
        assertEquals(0, snapshot.lastClosedPeriod!!.periodIndex)
        assertEquals(40L, snapshot.lastClosedPeriod!!.plan[BudgetDirection.REQUIRED])
        assertEquals(40L, snapshot.lastClosedPeriod!!.fact[BudgetDirection.REQUIRED])

        val income = db.earningDao().getByPeriod(profileId, 1)
            .filter { it.source == EconomyEngine.PERIOD_INCOME_SOURCE }
        assertEquals(1, income.size)
        assertEquals(EconomyEngine.PERIOD_INCOME_AMOUNT, income.first().amount)

        repository.saveBudgetPlan(plan(3, 1, 1))
        val after = freshRepository().loadSnapshot()!!
        assertEquals(PeriodStatus.PLANNED.name, after.currentPeriod?.status)
        assertEquals(1, after.currentPeriod?.periodIndex)
    }

    @Test
    fun `weak period still pays allowance but no bonus`() = runTest {
        repository.earn(source = "TASK", amount = 100L)
        repository.saveBudgetPlan(plan(40, 20, 25))
        assertTrue(repository.confirmBudgetPlan())
        // No purchases and no savings — period is weak.

        val closed = repository.closePeriod()
        assertTrue(closed is PeriodCloseResult.Success)
        val success = closed as PeriodCloseResult.Success
        assertFalse(success.review.isGoodPeriod)
        assertEquals(0, success.starsEarned)
        assertEquals(0, success.newStage)
        assertFalse(success.stageGrew)
        assertEquals(EconomyEngine.PERIOD_INCOME_AMOUNT, success.periodIncome)
        assertEquals(0L, success.planBonus)
        assertTrue(success.newMood < success.previousMood)

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(0, snapshot.profile.petStage)
        assertEquals(0, snapshot.profile.goodPeriods)
        assertEquals(
            GameRepository.START_BUDGET_AMOUNT + 100L + EconomyEngine.PERIOD_INCOME_AMOUNT,
            snapshot.balance,
        )
        val income = db.earningDao().getByPeriod(profileId, 1)
            .filter { it.source == EconomyEngine.PERIOD_INCOME_SOURCE }
        assertEquals(1, income.size)
    }

    @Test
    fun `stage grows after enough stars`() = runTest {
        repository.earn(source = "TASK", amount = 200L)
        assertTrue(repository.selectGoal("goal_1", "Велосипед", 500L))
        repeat(2) {
            repository.saveBudgetPlan(plan(10, 5, 5))
            assertTrue(repository.confirmBudgetPlan())
            repository.purchase(PurchaseDraft("food_basic", "Корм", PurchaseCategory.REQUIRED, 8L))
            repository.depositToSavings("goal_1", 5L)
            repository.closePeriod(listOf(WeekNeed("food_basic", "Корм", 8L)))
        }
        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(6, snapshot.profile.growthStars)
        assertEquals(1, snapshot.profile.petStage)
    }

    @Test
    fun `second try gives half reward`() = runTest {
        repository.completeTask("task_x", "Задание", "BUDGET", isCorrect = false, reward = 10L)
        val retry = repository.completeTask("task_x", "Задание", "BUDGET", isCorrect = true, reward = 10L)
        val success = retry as TaskCompletionResult.Success
        assertEquals(5L, success.reward)
        assertEquals(10L, success.fullReward)
        assertFalse(success.firstTry)
    }

    @Test
    fun `ledger lists income purchases and savings`() = runTest {
        repository.earn(source = "TASK", amount = 20L)
        repository.purchase(PurchaseDraft("food_basic", "Корм", PurchaseCategory.REQUIRED, 8L))
        assertTrue(repository.selectGoal("goal_1", "Велосипед", 60L))
        repository.depositToSavings("goal_1", 10L)

        val ledger = repository.loadLedger()
        assertEquals(listOf(30L, 20L, -8L, -10L), ledger.map { it.amount })
        assertEquals(LedgerKind.TO_SAVINGS, ledger.last().kind)
    }

    @Test
    fun `closePeriod requires active confirmed plan`() = runTest {
        val result = repository.closePeriod()
        assertTrue(result is PeriodCloseResult.Invalid)

        repository.earn(source = "TASK", amount = 50L)
        repository.saveBudgetPlan(plan(20, 10, 10))
        val stillPlanned = repository.closePeriod()
        assertTrue(stillPlanned is PeriodCloseResult.Invalid)
    }

    @Test
    fun `deleteCurrentProfile clears data so snapshot is null`() = runTest {
        repository.earn(source = "TASK", amount = 20L)
        assertTrue(repository.deleteCurrentProfile())
        assertNull(freshRepository().loadSnapshot())
    }

    @Test
    fun `test profile reset returns game to initial state`() = runTest {
        val initial = repository.loadSnapshot()!!
        val initialLedgerSize = repository.loadLedger().size
        repository.setAllTasksOpen(true)
        repository.completeTask("task_budget_1", "Первые монеты", "BUDGET", true, 5L)
        repository.purchase(PurchaseDraft("food_1", "Корм", PurchaseCategory.REQUIRED, 10L))
        assertTrue(repository.selectGoal("goal_1", "Велосипед", 200L))
        repository.depositToSavings("goal_1", 5L)
        repository.closePeriod()

        assertTrue(repository.deleteCurrentProfile())
        repository.createProfile(playerName = "Аня", petName = "Финни")
        val restarted = freshRepository().loadSnapshot()!!

        assertEquals(initial.balance, restarted.balance)
        assertEquals(0L, restarted.savingsTotal)
        assertTrue(restarted.savings.isEmpty())
        assertNull(restarted.selectedGoalId)
        assertTrue(restarted.achievedGoalIds.isEmpty())
        assertTrue(restarted.completedTasks.isEmpty())
        assertTrue(restarted.periodPurchases.isEmpty())
        assertEquals(0, restarted.closedPeriodCount)
        assertEquals(initial.currentPeriod?.periodIndex, restarted.currentPeriod?.periodIndex)
        assertEquals(initial.profile.growthStars, restarted.profile.growthStars)
        assertEquals(initial.profile.petStage, restarted.profile.petStage)
        assertFalse(restarted.profile.allTasksOpen)
        assertEquals(initialLedgerSize, repository.loadLedger().size)
    }

    @Test
    fun `demo coins top the balance up to the purse and do not stack`() = runTest {
        val added = repository.grantDemoCoins(100L)
        assertEquals(70L, added)
        assertEquals(100L, freshRepository().loadSnapshot()!!.balance)

        val again = repository.grantDemoCoins(100L)
        assertEquals(0L, again)
        assertEquals(100L, freshRepository().loadSnapshot()!!.balance)

        val titles = repository.loadLedger().map { it.title }
        assertTrue(GameRepository.DEMO_COINS_SOURCE in titles)
    }

    @Test
    fun `demo mode flag persists`() = runTest {
        assertTrue(repository.setAllTasksOpen(true))
        assertTrue(freshRepository().loadSnapshot()!!.profile.allTasksOpen)
        assertTrue(repository.setAllTasksOpen(false))
        assertFalse(freshRepository().loadSnapshot()!!.profile.allTasksOpen)
    }
}