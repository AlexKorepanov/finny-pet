package ru.finny.petgame.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.economy.model.BudgetPlan
import ru.finny.petgame.economy.model.DepositResult
import ru.finny.petgame.economy.model.EarnResult
import ru.finny.petgame.economy.model.PlanCheckResult
import ru.finny.petgame.economy.model.PurchaseCategory
import ru.finny.petgame.economy.model.PurchaseDraft
import ru.finny.petgame.economy.model.PurchaseResult
import ru.finny.petgame.economy.model.SavingsBucket
import ru.finny.petgame.economy.model.WithdrawResult

class EarningTest {

    private val engine = EconomyEngine()

    @Test
    fun `earning adds balance and records source with amount`() {
        val result = engine.earn(EconomyState(balance = 50L), source = "TASK", amount = 25L)

        assertTrue(result is EarnResult.Success)
        val success = result as EarnResult.Success
        assertEquals(75L, success.state.balance)
        assertEquals("TASK", success.earning.source)
        assertEquals(25L, success.earning.amount)
    }

    @Test
    fun `zero or negative earning is rejected`() {
        assertTrue(engine.earn(EconomyState(), "TASK", 0L) is EarnResult.Error)
        assertTrue(engine.earn(EconomyState(), "TASK", -10L) is EarnResult.Error)
    }

    @Test
    fun `earning without source is rejected`() {
        assertTrue(engine.earn(EconomyState(), source = "", amount = 10L) is EarnResult.Error)
        assertTrue(engine.earn(EconomyState(), source = "  ", amount = 10L) is EarnResult.Error)
    }
}

class EconomyFlowTest {

    private val engine = EconomyEngine()
    private val goalId = "goal_1"

    @Test
    fun `full cycle keeps balance non negative`() {
        var state = EconomyState()

        val earned = engine.earn(state, source = "TASK", amount = 100L)
        state = (earned as EarnResult.Success).state
        assertEquals(100L, state.balance)

        val plan = BudgetPlan(
            mapOf(
                BudgetDirection.REQUIRED to 40L,
                BudgetDirection.OPTIONAL to 20L,
                BudgetDirection.SAVINGS to 25L,
            ),
        )
        assertTrue(engine.checkPlan(plan, available = state.balance) is PlanCheckResult.Valid)

        val purchased = engine.purchase(
            state,
            PurchaseDraft("food_1", "Корм", PurchaseCategory.REQUIRED, 40L),
        )
        state = (purchased as PurchaseResult.Success).state
        assertEquals(60L, state.balance)

        val tooExpensive = engine.purchase(
            state,
            PurchaseDraft("toy_1", "Мяч", PurchaseCategory.OPTIONAL, 100L),
        )
        assertTrue(tooExpensive is PurchaseResult.InsufficientFunds)
        assertEquals(40L, (tooExpensive as PurchaseResult.InsufficientFunds).shortfall)
        assertEquals(60L, state.balance)

        state = state.copy(
            savings = mapOf(goalId to SavingsBucket(goalId, "Велосипед", 200L, 0L)),
        )
        val deposit = engine.depositToSavings(state, goalId, 25L)
        state = (deposit as DepositResult.Success).state
        assertEquals(35L, state.balance)

        val tooMuch = engine.withdrawFromSavings(state, goalId, 30L, confirmed = true)
        assertTrue(tooMuch is WithdrawResult.Invalid)
        assertEquals(35L, state.balance)

        val preview = engine.withdrawFromSavings(state, goalId, 10L, confirmed = false)
        assertTrue(preview is WithdrawResult.NeedsConfirmation)
        assertEquals(15L, (preview as WithdrawResult.NeedsConfirmation).newSavedAmount)

        val withdrawn = engine.withdrawFromSavings(state, goalId, 10L, confirmed = true)
        state = (withdrawn as WithdrawResult.Success).state
        assertEquals(45L, state.balance)
        assertEquals(15L, state.savings.getValue(goalId).savedAmount)

        assertTrue(state.balance >= 0L)
        assertTrue(state.savings.values.all { it.savedAmount >= 0L })
    }
}