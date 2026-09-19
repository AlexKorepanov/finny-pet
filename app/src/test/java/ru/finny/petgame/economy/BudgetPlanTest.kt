package ru.finny.petgame.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.economy.model.BudgetPlan
import ru.finny.petgame.economy.model.PlanCheckResult

class BudgetPlanTest {

    private val engine = EconomyEngine()

    @Test
    fun `valid plan is accepted with remainder`() {
        val plan = BudgetPlan(
            mapOf(
                BudgetDirection.REQUIRED to 30L,
                BudgetDirection.OPTIONAL to 20L,
                BudgetDirection.SAVINGS to 25L,
            ),
        )
        val result = engine.checkPlan(plan, available = 100L)

        assertTrue(result is PlanCheckResult.Valid)
        assertEquals(25L, (result as PlanCheckResult.Valid).remainder)
    }

    @Test
    fun `plan equal to available is accepted with zero remainder`() {
        val plan = BudgetPlan(
            mapOf(
                BudgetDirection.REQUIRED to 50L,
                BudgetDirection.OPTIONAL to 30L,
                BudgetDirection.SAVINGS to 20L,
            ),
        )
        val result = engine.checkPlan(plan, available = 100L)

        assertTrue(result is PlanCheckResult.Valid)
        assertEquals(0L, (result as PlanCheckResult.Valid).remainder)
    }

    @Test
    fun `plan above available is rejected with shortfall`() {
        val plan = BudgetPlan(
            mapOf(
                BudgetDirection.REQUIRED to 60L,
                BudgetDirection.OPTIONAL to 40L,
                BudgetDirection.SAVINGS to 40L,
            ),
        )
        val result = engine.checkPlan(plan, available = 100L)

        assertTrue(result is PlanCheckResult.Invalid)
        val problems = (result as PlanCheckResult.Invalid).problems
        assertTrue(problems.any { it.contains("40") })
    }

    @Test
    fun `missing direction is rejected`() {
        val plan = BudgetPlan(
            mapOf(
                BudgetDirection.REQUIRED to 60L,
                BudgetDirection.OPTIONAL to 40L,
            ),
        )
        val result = engine.checkPlan(plan, available = 100L)

        assertTrue(result is PlanCheckResult.Invalid)
        assertEquals(1, (result as PlanCheckResult.Invalid).problems.size)
    }

    @Test
    fun `zero amount in direction is rejected`() {
        val plan = BudgetPlan(
            mapOf(
                BudgetDirection.REQUIRED to 60L,
                BudgetDirection.OPTIONAL to 40L,
                BudgetDirection.SAVINGS to 0L,
            ),
        )
        val result = engine.checkPlan(plan, available = 100L)

        assertTrue(result is PlanCheckResult.Invalid)
        assertEquals(1, (result as PlanCheckResult.Invalid).problems.size)
    }

    @Test
    fun `negative amount is rejected`() {
        val plan = BudgetPlan(
            mapOf(
                BudgetDirection.REQUIRED to 60L,
                BudgetDirection.OPTIONAL to -10L,
                BudgetDirection.SAVINGS to 50L,
            ),
        )
        val result = engine.checkPlan(plan, available = 100L)

        assertTrue(result is PlanCheckResult.Invalid)
        assertEquals(1, (result as PlanCheckResult.Invalid).problems.size)
    }
}