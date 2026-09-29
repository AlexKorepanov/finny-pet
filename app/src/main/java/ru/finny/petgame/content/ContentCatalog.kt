package ru.finny.petgame.content

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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

    /**
     * Задание открыто, если его неделя уже прошла,
     * а в текущей неделе наступил его день. Демо-режим открывает всё.
     */
    fun isTaskOpen(task: TaskContent, periodIndex: Int, openDay: Int, allOpen: Boolean): Boolean {
        if (allOpen) return true
        val currentWeek = weekNumber(periodIndex)
        return when {
            task.week < currentWeek -> true
            task.week > currentWeek -> false
            else -> task.day <= openDay.coerceIn(1, DAYS_IN_WEEK)
        }
    }

    /**
     * Сколько монет нужно, чтобы купить всё по желанию и отложить на каждую мечту.
     * Так можно посмотреть аксессуары в магазине и вещи на полянке.
     */
    fun accessoryPurse(): Long =
        shopItems.filter { it.category == PurchaseCategory.OPTIONAL }.sumOf { it.price } +
            goals.sumOf { it.cost }

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
            val weekTasks = tasks.filter { it.week == week.index }
            if (weekTasks.size != TASKS_PER_WEEK) {
                problems += "weeks[${week.index}]: нужно $TASKS_PER_WEEK заданий, сейчас ${weekTasks.size}"
            }
            (1..DAYS_IN_WEEK).forEach { day ->
                val count = weekTasks.count { it.day == day }
                val expected = tasksPerDay(day)
                if (count != expected) {
                    problems += "weeks[${week.index}]: на день $day нужно $expected задания(й), сейчас $count"
                }
            }
        }
        return problems
    }
}

/** Учебная неделя: 1 — понедельник … 7 — воскресенье. */
const val DAYS_IN_WEEK = 7
const val WEEKDAY_TASKS = 5

/** В выходные уроки короче. */
const val WEEKEND_TASKS = 3
const val TASKS_PER_WEEK = 5 * WEEKDAY_TASKS + 2 * WEEKEND_TASKS

fun tasksPerDay(day: Int): Int = if (day <= 5) WEEKDAY_TASKS else WEEKEND_TASKS

/** Какой день недели уже наступил: 1 в день старта периода, 2 на следующий календарный день. */
fun openTaskDay(
    periodStartedAt: Long,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
): Int {
    val start = Instant.ofEpochMilli(periodStartedAt).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val passed = ChronoUnit.DAYS.between(start, today).toInt()
    return (passed + 1).coerceIn(1, DAYS_IN_WEEK)
}
