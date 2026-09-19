package ru.finny.petgame.economy.model

enum class PurchaseCategory(val label: String) {
    REQUIRED("Обязательная покупка"),
    OPTIONAL("Необязательная покупка"),
}