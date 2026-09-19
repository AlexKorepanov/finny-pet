package ru.finny.petgame.economy

import ru.finny.petgame.economy.model.SavingsBucket

data class EconomyState(
    val balance: Long = 0L,
    val savings: Map<String, SavingsBucket> = emptyMap(),
)