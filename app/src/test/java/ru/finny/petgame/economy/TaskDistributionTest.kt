package ru.finny.petgame.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.economy.model.DistributionCheck

class TaskDistributionTest {

    private val engine = EconomyEngine()

    private fun amounts(required: Long, optional: Long, savings: Long) = mapOf(
        BudgetDirection.REQUIRED to required,
        BudgetDirection.OPTIONAL to optional,
        BudgetDirection.SAVINGS to savings,
    )

    @Test
    fun `distribution is valid for exact sum within minimums`() {
        val check = engine.checkDistribution(
            amounts(8, 1, 1),
            sum = 10L,
            minimums = mapOf(BudgetDirection.REQUIRED to 5L, BudgetDirection.SAVINGS to 1L),
        )
        assertTrue(check is DistributionCheck.Valid)
        assertEquals(0L, (check as DistributionCheck.Valid).remainder)
    }

    @Test
    fun `distribution check rejects wrong sum`() {
        val check = engine.checkDistribution(amounts(5, 5, 5), sum = 10L, minimums = emptyMap())
        assertTrue(check is DistributionCheck.Invalid)
        assertTrue((check as DistributionCheck.Invalid).problems.isNotEmpty())
    }

    @Test
    fun `distribution check rejects below minimum`() {
        val check = engine.checkDistribution(
            amounts(2, 4, 4),
            sum = 10L,
            minimums = mapOf(BudgetDirection.REQUIRED to 5L),
        )
        assertTrue(check is DistributionCheck.Invalid)
        assertEquals(1, (check as DistributionCheck.Invalid).problems.size)
    }
}