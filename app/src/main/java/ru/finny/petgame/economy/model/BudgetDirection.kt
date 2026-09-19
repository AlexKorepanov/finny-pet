package ru.finny.petgame.economy.model

enum class BudgetDirection(val label: String) {
    REQUIRED("Обязательные расходы"),
    OPTIONAL("Необязательные расходы"),
    SAVINGS("Накопления"),
}