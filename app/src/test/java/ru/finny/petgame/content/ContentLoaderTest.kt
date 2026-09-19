package ru.finny.petgame.content

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
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
}