package ru.finny.petgame.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.finny.petgame.data.PetDatabase
import ru.finny.petgame.data.model.PeriodStatus
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
            petSpecies = 0,
            petColor = 1,
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
        assertEquals(0, snapshot.profile.petSpecies)
        assertEquals(1, snapshot.profile.petColor)
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
        assertTrue(repository.closePeriod())

        val snapshot = freshRepository().loadSnapshot()!!
        assertEquals(PeriodStatus.CLOSED.name, snapshot.currentPeriod?.status)
        assertEquals(fixedTime, snapshot.currentPeriod?.closedAt)

        repository.earn(source = "PERIOD_INCOME", amount = 5L)
        val earnings = db.earningDao().getByPeriod(profileId, 1)
        assertEquals(1, earnings.size)

        repository.saveBudgetPlan(plan(3, 1, 1))
        val after = freshRepository().loadSnapshot()!!
        assertEquals(PeriodStatus.PLANNED.name, after.currentPeriod?.status)
        assertEquals(1, after.currentPeriod?.periodIndex)
    }
}