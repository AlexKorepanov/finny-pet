package ru.finny.petgame.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlin.math.abs
import ru.finny.petgame.R

/** Форма слова выбирается по правилам русского языка, даже если язык телефона другой. */
@Composable
private fun russianPlural(count: Long, one: Int, few: Int, many: Int): String {
    val mod100 = abs(count) % 100
    val mod10 = abs(count) % 10
    val res = when {
        mod100 in 11..14 -> many
        mod10 == 1L -> one
        mod10 in 2..4 -> few
        else -> many
    }
    return stringResource(res, count)
}

@Composable
fun coinsAmount(amount: Long): String =
    russianPlural(amount, R.string.coins_one, R.string.coins_few, R.string.coins_many)

@Composable
fun starsAmount(count: Int): String =
    russianPlural(count.toLong(), R.string.stars_one, R.string.stars_few, R.string.stars_many)

@Composable
fun weeksAmount(count: Int): String =
    russianPlural(count.toLong(), R.string.weeks_one, R.string.weeks_few, R.string.weeks_many)
