package ru.finny.petgame.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.petgame.economy.model.SavingsBucket
import ru.finny.petgame.economy.model.WithdrawResult

class SavingsWithdrawTest {

    private val engine = EconomyEngine()
    private val goalId = "goal_1"

    private fun stateWithSavings(saved: Long): EconomyState =
        EconomyState(
            balance = 10L,
            savings = mapOf(goalId to SavingsBucket(goalId, "Велосипед", 200L, saved)),
        )

    @Test
    fun `withdraw without confirmation returns preview and keeps state`() {
        val before = stateWithSavings(saved = 80L)
        val result = engine.withdrawFromSavings(
            before,
            goalId,
            amount = 30L,
            confirmed = false,
            averageDeposit = 25L,
        )

        assertTrue(result is WithdrawResult.NeedsConfirmation)
        val preview = result as WithdrawResult.NeedsConfirmation
        assertEquals(50L, preview.newSavedAmount)
        assertEquals(6, preview.eta.periodsLeft)
        assertEquals(before, stateWithSavings(saved = 80L))
    }

    @Test
    fun `confirmed withdraw returns coins to balance`() {
        val result = engine.withdrawFromSavings(
            stateWithSavings(saved = 80L),
            goalId,
            amount = 30L,
            confirmed = true,
        )

        assertTrue(result is WithdrawResult.Success)
        val success = result as WithdrawResult.Success
        assertEquals(40L, success.state.balance)
        assertEquals(50L, success.bucket.savedAmount)
    }

    @Test
    fun `withdraw more than saved is rejected with explanation`() {
        val result = engine.withdrawFromSavings(
            stateWithSavings(saved = 30L),
            goalId,
            amount = 50L,
            confirmed = true,
        )

        assertTrue(result is WithdrawResult.Invalid)
        assertTrue((result as WithdrawResult.Invalid).explanation.contains("30"))
    }

    @Test
    fun `withdraw never makes savings negative`() {
        val result = engine.withdrawFromSavings(
            stateWithSavings(saved = 20L),
            goalId,
            amount = 100L,
            confirmed = true,
        )

        assertTrue(result is WithdrawResult.Invalid)
    }

    @Test
    fun `zero or negative withdraw is rejected`() {
        assertTrue(
            engine.withdrawFromSavings(stateWithSavings(50L), goalId, 0L, confirmed = true)
                is WithdrawResult.Invalid,
        )
        assertTrue(
            engine.withdrawFromSavings(stateWithSavings(50L), goalId, -5L, confirmed = true)
                is WithdrawResult.Invalid,
        )
    }

    @Test
    fun `withdraw from unknown goal is rejected`() {
        val result = engine.withdrawFromSavings(EconomyState(balance = 0L), "unknown", 10L, confirmed = true)

        assertTrue(result is WithdrawResult.Invalid)
    }
}