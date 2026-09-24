package ru.finny.petgame.content

import ru.finny.petgame.content.model.SavingsGoalContent
import ru.finny.petgame.content.model.ShopItemContent
import ru.finny.petgame.content.model.TaskContent
import ru.finny.petgame.content.model.WeekContent
import ru.finny.petgame.economy.model.PurchaseCategory
import ru.finny.petgame.economy.model.WeekNeed

class ContentCatalog(
    val tasks: List<TaskContent>,
    val shopItems: List<ShopItemContent>,
    val goals: List<SavingsGoalContent>,
    val weeks: List<WeekContent> = emptyList(),
) {

    fun taskById(id: String): TaskContent? = tasks.firstOrNull { it.id == id }

    fun shopItemById(id: String): ShopItemContent? = shopItems.firstOrNull { it.id == id }

    fun goalById(id: String): SavingsGoalContent? = goals.firstOrNull { it.id == id }

    /** Учебная неделя для периода; после последней недели список идёт по кругу. */
    fun weekFor(periodIndex: Int): WeekContent? =
        if (weeks.isEmpty()) null else weeks[periodIndex.coerceAtLeast(0) % weeks.size]

    /** Номер недели для показа ребёнку (1, 2, 3…), не зависит от круга. */
    fun weekNumber(periodIndex: Int): Int = periodIndex.coerceAtLeast(0) + 1

    fun needsFor(periodIndex: Int): List<WeekNeed> =
        weekFor(periodIndex)?.needs.orEmpty().mapNotNull { id ->
            shopItemById(id)?.let { item -> WeekNeed(itemId = item.id, title = item.title, price = item.price) }
        }

    /** Задание открыто, если его неделя уже наступила (или включён демо-режим). */
    fun isTaskOpen(task: TaskContent, periodIndex: Int, allOpen: Boolean): Boolean =
        allOpen || task.week <= weekNumber(periodIndex)

    fun tasksOfWeek(periodIndex: Int): List<TaskContent> {
        val cycleWeek = weekFor(periodIndex)?.index ?: weekNumber(periodIndex)
        return tasks.filter { it.week == cycleWeek }
    }

    /** Проверка связей между файлами контента: нужное недели ссылается на обязательные товары. */
    fun validate(): List<String> {
        val problems = mutableListOf<String>()
        weeks.forEach { week ->
            week.needs.forEach { id ->
                val item = shopItemById(id)
                when {
                    item == null -> problems += "weeks[${week.index}]: нет товара $id в shop.json"
                    item.category != PurchaseCategory.REQUIRED ->
                        problems += "weeks[${week.index}]: товар $id должен быть из нужного (REQUIRED)"
                }
            }
        }
        return problems
    }
}
