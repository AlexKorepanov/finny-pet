package ru.finny.petgame.content.model

import ru.finny.petgame.economy.model.BudgetDirection

enum class TaskTheme(val label: String) {
    BUDGET("Планирование бюджета"),
    SAVINGS("Сбережения"),
    PAYMENTS("Платежи и покупки"),
}

enum class TaskType(val label: String) {
    CHOICE("Выбор действия"),
    DISTRIBUTE("Распределение монет"),
}

data class TaskOption(
    val id: String,
    val text: String,
    val result: String,
    val isGood: Boolean,
)

data class TaskContent(
    val id: String,
    val theme: TaskTheme,
    val type: TaskType,
    val title: String,
    val story: String,
    val question: String,
    val reward: Long,
    val options: List<TaskOption> = emptyList(),
    val sum: Long = 0L,
    val minimums: Map<BudgetDirection, Long> = emptyMap(),
    val correctExplanation: String,
    val wrongExplanation: String,
    /** Номер учебной недели (с 1), с которой задание открыто. */
    val week: Int = 1,
)