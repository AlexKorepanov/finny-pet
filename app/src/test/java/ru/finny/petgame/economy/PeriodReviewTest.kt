package ru.finny.petgame.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.economy.model.WeekNeed

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

    private val food = WeekNeed("food_basic", "Корм", 8)
    private val bath = WeekNeed("bath", "Купание", 15)

    @Test
    fun `good period meets all three criteria`() {
        val review = engine.evaluatePeriod(plan(40, 20, 25), fact(40, 15, 25))

        assertTrue(review.requiredCovered)
        assertTrue(review.planMatched)
        assertTrue(review.savingsRegular)
        assertTrue(review.isGoodPeriod)
        assertEquals(3, review.stars)
        assertEquals(0, review.moodDelta)
        assertEquals(EconomyEngine.SATIETY_WEEK_DECAY, review.satietyDelta)
        assertEquals(5L, review.economized)
        assertTrue(review.explanations.any { it.contains("сэкономил 5 монет") })
    }

    @Test
    fun `missing required spending lowers mood and blocks good period`() {
        val review = engine.evaluatePeriod(plan(40, 20, 25), fact(0, 10, 25))

        assertFalse(review.requiredCovered)
        assertTrue(review.planMatched)
        assertTrue(review.savingsRegular)
        assertFalse(review.isGoodPeriod)
        assertEquals(2, review.stars)
        assertEquals(-8, review.moodDelta)
        assertTrue(review.explanations.any { it.contains("нужного") })
    }

    @Test
    fun `week needs decide required coverage`() {
        val needs = listOf(food, bath)
        val partial = engine.evaluatePeriod(plan(10, 10, 10), fact(8, 0, 10), needs, setOf("food_basic"))

        assertFalse(partial.requiredCovered)
        assertEquals(listOf("Купание"), partial.missingNeeds)
        assertTrue(partial.explanations.any { it.contains("Купание") })
        assertTrue(partial.advice.contains("23 монеты"))

        val full = engine.evaluatePeriod(plan(23, 0, 7), fact(23, 0, 7), needs, setOf("food_basic", "bath"))
        assertTrue(full.requiredCovered)
        assertTrue(full.missingNeeds.isEmpty())
        assertEquals(3, full.stars)
    }

    @Test
    fun `overspend breaks plan match and names the amount`() {
        val review = engine.evaluatePeriod(plan(40, 20, 25), fact(40, 30, 25))

        assertTrue(review.requiredCovered)
        assertFalse(review.planMatched)
        assertTrue(review.savingsRegular)
        assertFalse(review.isGoodPeriod)
        assertTrue(review.explanations.any { it.contains("на 10 монет больше плана") })
        assertTrue(review.advice.contains("осталось в плане"))
    }

    @Test
    fun `no savings is not regular`() {
        val review = engine.evaluatePeriod(plan(40, 20, 25), fact(40, 10, 0))

        assertTrue(review.requiredCovered)
        assertFalse(review.planMatched)
        assertFalse(review.savingsRegular)
        assertFalse(review.isGoodPeriod)
        assertEquals(1, review.stars)
        assertTrue(review.explanations.any { it.contains("копилку") })
    }

    @Test
    fun `stage follows total stars`() {
        assertEquals(0, engine.stageForStars(0))
        assertEquals(0, engine.stageForStars(3))
        assertEquals(1, engine.stageForStars(4))
        assertEquals(1, engine.stageForStars(8))
        assertEquals(2, engine.stageForStars(9))
        assertEquals(2, engine.stageForStars(15))
    }

    @Test
    fun `stars to next stage`() {
        assertEquals(4, engine.starsToNextStage(0))
        assertEquals(1, engine.starsToNextStage(3))
        assertEquals(5, engine.starsToNextStage(4))
        assertNull(engine.starsToNextStage(9))
    }

    @Test
    fun `task reward is halved after a mistake`() {
        assertEquals(10L, engine.taskReward(10, 0))
        assertEquals(5L, engine.taskReward(10, 1))
        assertEquals(3L, engine.taskReward(5, 2))
    }

    @Test
    fun `over plan amount`() {
        val plan = plan(20, 10, 5)
        val fact = fact(8, 6, 0)
        assertEquals(0L, engine.overPlanAmount(plan, fact, BudgetDirection.OPTIONAL, 4))
        assertEquals(8L, engine.overPlanAmount(plan, fact, BudgetDirection.OPTIONAL, 12))
    }
}
