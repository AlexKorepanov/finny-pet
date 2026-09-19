package ru.finny.petgame.economy

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.finny.petgame.economy.model.SavingsBucket

class GoalEtaTest {

    private val engine = EconomyEngine()

    @Test
    fun `average deposit is integer mean`() {
        assertEquals(20L, engine.averageDeposit(listOf(10L, 20L, 30L)))
        assertEquals(15L, engine.averageDeposit(listOf(10L, 20L)))
        assertEquals(7L, engine.averageDeposit(listOf(7L)))
        assertEquals(0L, engine.averageDeposit(emptyList()))
    }

    @Test
    fun `eta rounds up to whole periods`() {
        val bucket = SavingsBucket("goal_1", "Велосипед", 100L, 0L)
        val eta = engine.goalEta(bucket, averageDeposit = 30L)

        assertEquals(100L, eta.remaining)
        assertEquals(4, eta.periodsLeft)
    }

    @Test
    fun `eta for exact division is exact`() {
        val bucket = SavingsBucket("goal_1", "Велосипед", 90L, 0L)
        val eta = engine.goalEta(bucket, averageDeposit = 30L)

        assertEquals(3, eta.periodsLeft)
    }

    @Test
    fun `eta counts from current savings`() {
        val bucket = SavingsBucket("goal_1", "Велосипед", 100L, 40L)
        val eta = engine.goalEta(bucket, averageDeposit = 30L)

        assertEquals(60L, eta.remaining)
        assertEquals(2, eta.periodsLeft)
    }

    @Test
    fun `reached goal needs zero periods`() {
        val bucket = SavingsBucket("goal_1", "Велосипед", 100L, 100L)
        val eta = engine.goalEta(bucket, averageDeposit = 30L)

        assertEquals(0L, eta.remaining)
        assertEquals(0, eta.periodsLeft)
    }

    @Test
    fun `eta is unknown while there are no deposits yet`() {
        val bucket = SavingsBucket("goal_1", "Велосипед", 100L, 0L)
        val eta = engine.goalEta(bucket, averageDeposit = 0L)

        assertEquals(null, eta.periodsLeft)
    }
}