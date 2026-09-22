package ru.finny.petgame.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import ru.finny.petgame.R

@Composable
fun coinsAmount(amount: Long): String =
    pluralStringResource(R.plurals.coins, amount.toInt(), amount.toInt())

@Composable
fun timesAmount(count: Int): String =
    pluralStringResource(R.plurals.times, count, count)