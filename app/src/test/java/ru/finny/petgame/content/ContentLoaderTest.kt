package ru.finny.petgame.content

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import ru.finny.petgame.content.model.TaskType
import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.economy.model.PurchaseCategory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContentLoaderTest {

    private lateinit var loader: ContentLoader

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        loader = ContentLoader(context.assets)
    }

    @Test
    fun `catalog loads with minimum content amounts`() {
        val catalog = loader.loadCatalog()

        assertTrue(catalog.tasks.size >= 6)
        assertEquals(3, catalog.tasks.map { it.theme }.distinct().size)
        assertTrue(catalog.tasks.map { it.type }.distinct().size >= 2)
        assertTrue(catalog.shopItems.size >= 8)
        assertEquals(2, catalog.shopItems.map { it.category }.distinct().size)
        assertTrue(catalog.goals.size >= 3)
    }

    @Test
    fun `every task has texts explanations and per type fields`() {
        val catalog = loader.loadCatalog()

        catalog.tasks.forEach { task ->
            assertTrue(task.title.isNotBlank())
            assertTrue(task.story.isNotBlank())
            assertTrue(task.question.isNotBlank())
            assertTrue(task.correctExplanation.isNotBlank())
            assertTrue(task.wrongExplanation.isNotBlank())
            assertTrue(task.reward > 0L)
            when (task.type) {
                TaskType.CHOICE -> {
                    assertTrue(task.options.size >= 2)
                    assertTrue(task.options.any { it.isGood })
                    assertTrue(task.options.all { it.text.isNotBlank() && it.result.isNotBlank() })
                }
                TaskType.DISTRIBUTE -> {
                    assertTrue(task.sum > 0L)
                    assertTrue((task.minimums[BudgetDirection.SAVINGS] ?: 0L) >= 1L)
                }
            }
        }
    }

    @Test
    fun `ids are unique inside each content list`() {
        val catalog = loader.loadCatalog()

        assertEquals(catalog.tasks.size, catalog.tasks.map { it.id }.distinct().size)
        assertEquals(catalog.shopItems.size, catalog.shopItems.map { it.id }.distinct().size)
        assertEquals(catalog.goals.size, catalog.goals.map { it.id }.distinct().size)
    }

    @Test
    fun `shop items cover both categories with price and effect`() {
        val catalog = loader.loadCatalog()

        assertTrue(catalog.shopItems.any { it.category == PurchaseCategory.REQUIRED })
        assertTrue(catalog.shopItems.any { it.category == PurchaseCategory.OPTIONAL })
        catalog.shopItems.forEach { item ->
            assertTrue(item.price > 0L)
            assertTrue(item.effect.isNotBlank())
        }
    }

    @Test
    fun `goals have cost and texts`() {
        val catalog = loader.loadCatalog()

        assertTrue(catalog.goals.size >= 3)
        catalog.goals.forEach { goal ->
            assertTrue(goal.cost > 0L)
            assertTrue(goal.title.isNotBlank())
            assertTrue(goal.description.isNotBlank())
        }
    }

    @Test
    fun `catalog lookup helpers find items`() {
        val catalog = loader.loadCatalog()

        val task = catalog.tasks.first()
        assertEquals(task, catalog.taskById(task.id))
        val item = catalog.shopItems.first()
        assertEquals(item, catalog.shopItemById(item.id))
        val goal = catalog.goals.first()
        assertEquals(goal, catalog.goalById(goal.id))
    }

    @Test
    fun `weeks list needs from shop and every day of the week has tasks`() {
        val catalog = loader.loadCatalog()

        assertEquals(5, catalog.weeks.size)
        assertEquals(155, catalog.tasks.size)
        assertTrue(catalog.validate().isEmpty())
        catalog.weeks.forEach { week ->
            val weekTasks = catalog.tasksOfWeek(week.index - 1)
            assertEquals(31, weekTasks.size)
            (1..5).forEach { day ->
                assertEquals(5, weekTasks.count { it.day == day })
            }
            (6..7).forEach { day ->
                assertEquals(3, weekTasks.count { it.day == day })
            }
            assertTrue(catalog.needsFor(week.index - 1).any { it.itemId == "food_basic" })
        }
    }

    @Test
    fun `tasks open by week and weekday unless all opened`() {
        val catalog = loader.loadCatalog()
        val monday = catalog.tasks.first { it.week == 1 && it.day == 1 }
        val tuesday = catalog.tasks.first { it.week == 1 && it.day == 2 }
        val late = catalog.tasks.first { it.week == 5 && it.day == 1 }
        val lateTuesday = catalog.tasks.first { it.week == 5 && it.day == 2 }

        assertTrue(catalog.isTaskOpen(monday, periodIndex = 0, openDay = 1, allOpen = false))
        assertFalse(catalog.isTaskOpen(tuesday, periodIndex = 0, openDay = 1, allOpen = false))
        assertTrue(catalog.isTaskOpen(tuesday, periodIndex = 0, openDay = 2, allOpen = false))
        assertFalse(catalog.isTaskOpen(late, periodIndex = 0, openDay = 5, allOpen = false))
        assertTrue(catalog.isTaskOpen(late, periodIndex = 4, openDay = 1, allOpen = false))
        assertFalse(catalog.isTaskOpen(lateTuesday, periodIndex = 4, openDay = 1, allOpen = false))
        assertTrue(catalog.isTaskOpen(tuesday, periodIndex = 1, openDay = 1, allOpen = false))
        assertTrue(catalog.isTaskOpen(tuesday, periodIndex = 0, openDay = 1, allOpen = true))
        val sunday = catalog.tasks.first { it.week == 1 && it.day == 7 }
        assertFalse(catalog.isTaskOpen(sunday, periodIndex = 0, openDay = 6, allOpen = false))
        assertTrue(catalog.isTaskOpen(sunday, periodIndex = 0, openDay = 7, allOpen = false))
        assertEquals(catalog.weeks.first(), catalog.weekFor(5))
        assertEquals(2_090L, catalog.accessoryPurse())
    }

    @Test
    fun `weekday opens on the next calendar day not after 24 hours`() {
        val zone = ZoneId.of("Europe/Moscow")
        val start = LocalDate.of(2026, 9, 27).atTime(LocalTime.of(23, 0)).atZone(zone).toInstant().toEpochMilli()
        val laterSameNight = LocalDate.of(2026, 9, 27).atTime(LocalTime.of(23, 40)).atZone(zone).toInstant().toEpochMilli()
        val nextMorning = LocalDate.of(2026, 9, 28).atTime(LocalTime.of(0, 10)).atZone(zone).toInstant().toEpochMilli()
        val nextWeek = LocalDate.of(2026, 10, 4).atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()

        assertEquals(1, openTaskDay(start, laterSameNight, zone))
        assertEquals(2, openTaskDay(start, nextMorning, zone))
        assertEquals(7, openTaskDay(start, nextWeek, zone))
    }
}
