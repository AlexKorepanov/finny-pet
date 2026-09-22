package ru.finny.petgame.economy.model

enum class BudgetDirection(val label: String) {
    REQUIRED("Нужное (еда и уход)"),
    OPTIONAL("По желанию"),
    SAVINGS("Копилка"),
}