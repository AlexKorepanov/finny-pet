package ru.finny.petgame.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.petgame.economy.model.DepositResult
import ru.finny.petgame.economy.model.SavingsBucket

class SavingsDepositTest {

    private val engine = EconomyEngine()
    private val goalId = "goal_1"

    private fun stateWithGoal(balance: Long = 100L): EconomyState =
        EconomyState(
            balance = balance,
            savings = mapOf(goalId to SavingsBucket(goalId, "Велосипед", 200L, 0L)),
        )

    @Test
    fun `deposit moves coins from balance to savings`() {
        val result = engine.depositToSavings(stateWithGoal(), goalId, 30L)

        assertTrue(result is DepositResult.Success)
        val success = result as DepositResult.Success
        assertEquals(70L, success.state.balance)
        assertEquals(30L, success.bucket.savedAmount)
    }

    @Test
    fun `deposit more than balance is rejected without going negative`() {
        val result = engine.depositToSavings(stateWithGoal(balance = 20L), goalId, 50L)

        assertTrue(result is DepositResult.InsufficientFunds)
        val failed = result as DepositResult.InsufficientFunds
        assertEquals(30L, failed.shortfall)
        assertEquals(20L, failed.balance)
    }

    @Test
    fun `zero or negative deposit is rejected`() {
        assertTrue(engine.depositToSavings(stateWithGoal(), goalId, 0L) is DepositResult.Invalid)
        assertTrue(engine.depositToSavings(stateWithGoal(), goalId, -10L) is DepositResult.Invalid)
    }

    @Test
    fun `deposit to unknown goal is rejected`() {
        val result = engine.depositToSavings(EconomyState(balance = 100L), "unknown", 10L)

        assertTrue(result is DepositResult.Invalid)
    }

    @Test
    fun `deposit up to goal cost leaves zero remaining`() {
        val result = engine.depositToSavings(stateWithGoal(balance = 250L), goalId, 200L)

        assertTrue(result is DepositResult.Success)
        assertEquals(200L, (result as DepositResult.Success).bucket.savedAmount)
        assertEquals(0L, result.bucket.remaining)
    }
}