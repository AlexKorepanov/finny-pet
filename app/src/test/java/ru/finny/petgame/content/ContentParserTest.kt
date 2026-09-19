package ru.finny.petgame.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.finny.petgame.content.model.TaskTheme
import ru.finny.petgame.content.model.TaskType
import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.economy.model.PurchaseCategory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContentParserTest {

    private val choiceBody = """
        {
          "id": "t1",
          "theme": "BUDGET",
          "type": "CHOICE",
          "title": "Заголовок",
          "story": "История",
          "question": "Вопрос?",
          "reward": 5,
          "options": [
            {"id": "a", "text": "Первый", "result": "Плохо", "isGood": false},
            {"id": "b", "text": "Второй", "result": "Хорошо", "isGood": true}
          ],
          "correctExplanation": "Верно",
          "wrongExplanation": "Не верно"
        }
    """

    private val distributeBody = """
        {
          "id": "t2",
          "theme": "SAVINGS",
          "type": "DISTRIBUTE",
          "title": "Заголовок",
          "story": "История",
          "question": "Вопрос?",
          "reward": 10,
          "sum": 20,
          "minimums": {"REQUIRED": 5},
          "correctExplanation": "Верно",
          "wrongExplanation": "Не верно"
        }
    """

    private fun wrap(vararg bodies: String): String =
        """{ "tasks": [ ${bodies.joinToString(",")} ] }"""

    @Test
    fun `valid choice task is parsed with all fields`() {
        val tasks = ContentParser.parseTasks(wrap(choiceBody))

        assertEquals(1, tasks.size)
        val task = tasks[0]
        assertEquals("t1", task.id)
        assertEquals(TaskTheme.BUDGET, task.theme)
        assertEquals(TaskType.CHOICE, task.type)
        assertEquals(5L, task.reward)
        assertEquals(2, task.options.size)
        assertEquals("a", task.options[0].id)
        assertEquals("Первый", task.options[0].text)
        assertEquals("Плохо", task.options[0].result)
        assertEquals(false, task.options[0].isGood)
        assertEquals("Верно", task.correctExplanation)
        assertEquals("Не верно", task.wrongExplanation)
        assertEquals(0L, task.sum)
    }

    @Test
    fun `valid distribute task is parsed with sum and minimums`() {
        val task = ContentParser.parseTasks(wrap(distributeBody)).single()

        assertEquals(TaskType.DISTRIBUTE, task.type)
        assertEquals(20L, task.sum)
        assertEquals(mapOf(BudgetDirection.REQUIRED to 5L), task.minimums)
        assertTrue(task.options.isEmpty())
    }

    @Test
    fun `missing required field is reported with path`() {
        val json = wrap(choiceBody.replace("\"reward\": 5,", ""))

        val error = assertThrows(ContentFormatException::class.java) {
            ContentParser.parseTasks(json)
        }
        assertTrue(error.problems.any { it.contains("tasks[0]") && it.contains("reward") })
    }

    @Test
    fun `unknown theme value is reported`() {
        val json = wrap(choiceBody.replace("BUDGET", "MATH"))

        val error = assertThrows(ContentFormatException::class.java) {
            ContentParser.parseTasks(json)
        }
        assertTrue(error.problems.any { it.contains("theme") && it.contains("MATH") })
    }

    @Test
    fun `non positive reward is reported`() {
        val json = wrap(choiceBody.replace("\"reward\": 5", "\"reward\": 0"))

        val error = assertThrows(ContentFormatException::class.java) {
            ContentParser.parseTasks(json)
        }
        assertTrue(error.problems.any { it.contains("reward") })
    }

    @Test
    fun `choice task needs at least two options and one good`() {
        val singleOption = choiceBody.replace(
            """{"id": "a", "text": "Первый", "result": "Плохо", "isGood": false},""",
            "",
        )
        val first = assertThrows(ContentFormatException::class.java) {
            ContentParser.parseTasks(wrap(singleOption))
        }
        assertTrue(first.problems.any { it.contains("минимум два варианта") })

        val noGood = choiceBody.replace("\"isGood\": true", "\"isGood\": false")
        val second = assertThrows(ContentFormatException::class.java) {
            ContentParser.parseTasks(wrap(noGood))
        }
        assertTrue(second.problems.any { it.contains("правильный вариант") })
    }

    @Test
    fun `distribute task without sum is reported`() {
        val json = wrap(distributeBody.replace("\"sum\": 20,", ""))

        val error = assertThrows(ContentFormatException::class.java) {
            ContentParser.parseTasks(json)
        }
        assertTrue(error.problems.any { it.contains("sum") })
    }

    @Test
    fun `unknown minimum direction is reported`() {
        val json = wrap(distributeBody.replace("\"REQUIRED\": 5", "\"TOYS\": 5"))

        val error = assertThrows(ContentFormatException::class.java) {
            ContentParser.parseTasks(json)
        }
        assertTrue(error.problems.any { it.contains("TOYS") })
    }

    @Test
    fun `duplicate task id is reported`() {
        val error = assertThrows(ContentFormatException::class.java) {
            ContentParser.parseTasks(wrap(distributeBody, distributeBody))
        }
        assertTrue(error.problems.any { it.contains("повторяется id") })
    }

    @Test
    fun `valid shop item is parsed with defaults`() {
        val json = """{ "items": [ {
            "id": "i1", "title": "Корм", "category": "REQUIRED",
            "price": 10, "effect": "Финни сыт"
        } ] }"""

        val items = ContentParser.parseShopItems(json)

        assertEquals(1, items.size)
        assertEquals(PurchaseCategory.REQUIRED, items[0].category)
        assertEquals(10L, items[0].price)
        assertEquals(0, items[0].moodDelta)
    }

    @Test
    fun `shop item with bad price or category is rejected`() {
        val badPrice = """{ "items": [ {
            "id": "i1", "title": "Корм", "category": "REQUIRED",
            "price": 0, "effect": "Финни сыт"
        } ] }"""
        val error = assertThrows(ContentFormatException::class.java) {
            ContentParser.parseShopItems(badPrice)
        }
        assertTrue(error.problems.any { it.contains("price") })

        val badCategory = """{ "items": [ {
            "id": "i1", "title": "Корм", "category": "SOMETIMES",
            "price": 10, "effect": "Финни сыт"
        } ] }"""
        val error2 = assertThrows(ContentFormatException::class.java) {
            ContentParser.parseShopItems(badCategory)
        }
        assertTrue(error2.problems.any { it.contains("category") })
    }

    @Test
    fun `goal is parsed and validates cost`() {
        val valid = """{ "goals": [ {
            "id": "g1", "title": "Домик", "cost": 40, "description": "Уютный"
        } ] }"""
        val goals = ContentParser.parseGoals(valid)

        assertEquals(1, goals.size)
        assertEquals(40L, goals[0].cost)

        val noCost = """{ "goals": [ {
            "id": "g1", "title": "Домик", "description": "Уютный"
        } ] }"""
        val error = assertThrows(ContentFormatException::class.java) {
            ContentParser.parseGoals(noCost)
        }
        assertTrue(error.problems.any { it.contains("cost") })
    }

    @Test
    fun `broken json and missing array are reported`() {
        assertThrows(ContentFormatException::class.java) {
            ContentParser.parseTasks("не json")
        }
        assertThrows(ContentFormatException::class.java) {
            ContentParser.parseTasks("""{ "wrong": [] }""")
        }
    }
}