package ru.finny.petgame.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.petgame.economy.model.BudgetDirection

class PeriodReviewTest {

    private val engine = EconomyEngine()

    private fun plan(required: Long, optional: Long, savings: Long) = mapOf(
        BudgetDirection.REQUIRED to required,
        BudgetDirection.OPTIONAL to optional,
        BudgetDirection.SAVINGS to savings,
    )

    private fun fact(required: Long, optional: Long, savings: Long) = mapOf(
        BudgetDirection.REQUIRED to required,
        BudgetDirection.OPTIONAL to optional,
        BudgetDirection.SAVINGS to savings,
    )

    @Test
    fun `good period meets all three criteria`() {
        val review = engine.evaluatePeriod(plan(40, 20, 25), fact(40, 15, 25))

        assertTrue(review.requiredCovered)
        assertTrue(review.planMatched)
        assertTrue(review.savingsRegular)
        assertTrue(review.isGoodPeriod)
        assertEquals(18, review.moodDelta)
        assertEquals(3, review.explanations.size)
    }

    @Test
    fun `missing required spending lowers mood and blocks growth`() {
        val review = engine.evaluatePeriod(plan(40, 20, 25), fact(10, 10, 25))

        assertFalse(review.requiredCovered)
        assertTrue(review.planMatched)
        assertTrue(review.savingsRegular)
        assertFalse(review.isGoodPeriod)
        assertEquals(2, review.moodDelta)
        assertTrue(review.explanations.any { it.contains("Нужного") })
    }

    @Test
    fun `overspend breaks plan match`() {
        val review = engine.evaluatePeriod(plan(40, 20, 25), fact(40, 30, 25))

        assertTrue(review.requiredCovered)
        assertFalse(review.planMatched)
        assertTrue(review.savingsRegular)
        assertFalse(review.isGoodPeriod)
        assertTrue(review.explanations.any { it.contains("не совпал") })
    }

    @Test
    fun `no savings is not regular`() {
        val review = engine.evaluatePeriod(plan(40, 20, 25), fact(40, 10, 0))

        assertTrue(review.requiredCovered)
        assertFalse(review.planMatched)
        assertFalse(review.savingsRegular)
        assertFalse(review.isGoodPeriod)
        assertTrue(review.explanations.any { it.contains("копилку") })
    }

    @Test
    fun `stage grows only on good period up to max`() {
        assertEquals(0, engine.nextPetStage(0, isGoodPeriod = false))
        assertEquals(1, engine.nextPetStage(0, isGoodPeriod = true))
        assertEquals(2, engine.nextPetStage(1, isGoodPeriod = true))
        assertEquals(2, engine.nextPetStage(2, isGoodPeriod = true))
    }
}
