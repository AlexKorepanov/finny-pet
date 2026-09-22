package ru.finny.petgame.economy.model

sealed interface DistributionCheck {
    data class Valid(val remainder: Long) : DistributionCheck
    data class Invalid(val problems: List<String>) : DistributionCheck
}