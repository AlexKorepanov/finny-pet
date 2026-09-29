package ru.finny.petgame.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import ru.finny.petgame.R
import java.util.Calendar

/**
 * Время суток на полянке: своя картинка заставки и леса, свои цвета надписей,
 * полосы загрузки и листьев, которыми закрывается экран.
 */
enum class TimeOfDay(
    @DrawableRes val splash: Int,
    @DrawableRes val forest: Int,
    /** Картинка сама по себе тёмная — в тёмной теме её не нужно приглушать. */
    val darkScene: Boolean,
    val splashInk: Color,
    val splashTextShadow: Color,
    val splashBarTrack: Color,
    val splashBarBorder: Color,
    val splashBarFill: Color,
    val leafColors: List<Color>,
    val leafVein: Color,
    val curtainFill: Color,
) {
    DAY(
        splash = R.drawable.splash_day,
        forest = R.drawable.scene_forest_day,
        darkScene = false,
        splashInk = Color(0xFF5A3A1A),
        splashTextShadow = Color(0xCCFFF8EC),
        splashBarTrack = Color(0xFFFFF8EC),
        splashBarBorder = Color(0xFF5A3A1A),
        splashBarFill = Color(0xFFE39A3C),
        leafColors = listOf(
            Color(0xFF7FA23A),
            Color(0xFF93B548),
            Color(0xFF6B8F2E),
            Color(0xFFA9C25A),
            Color(0xFF5E8129),
            Color(0xFFB9C96A),
        ),
        leafVein = Color(0xFF4A6A1E),
        curtainFill = Color(0xFF6E9334),
    ),
    EVENING(
        splash = R.drawable.splash_evening,
        forest = R.drawable.scene_forest_evening,
        darkScene = true,
        splashInk = Color(0xFFFFEBD2),
        splashTextShadow = Color(0x99401E2A),
        splashBarTrack = Color(0xFF3E2638),
        splashBarBorder = Color(0xFFFFEBD2),
        splashBarFill = Color(0xFFFF9E4A),
        leafColors = listOf(
            Color(0xFFC77B3A),
            Color(0xFFE0954A),
            Color(0xFF9C5A3A),
            Color(0xFF8A7A3C),
            Color(0xFFB0563E),
            Color(0xFF7E6A40),
        ),
        leafVein = Color(0xFF4A2A1E),
        curtainFill = Color(0xFF6A4234),
    ),
    NIGHT(
        splash = R.drawable.splash_night,
        forest = R.drawable.scene_forest_night,
        darkScene = true,
        splashInk = Color(0xFFF6EBD0),
        splashTextShadow = Color(0x990A1026),
        splashBarTrack = Color(0xFF1B2444),
        splashBarBorder = Color(0xFFE9DFC4),
        splashBarFill = Color(0xFFF3D98B),
        leafColors = listOf(
            Color(0xFF2F5A5E),
            Color(0xFF3C6C66),
            Color(0xFF264A55),
            Color(0xFF4B7A70),
            Color(0xFF1F3F4F),
            Color(0xFF577F86),
        ),
        leafVein = Color(0xFF12262F),
        curtainFill = Color(0xFF1E3A48),
    ),
    ;

    companion object {
        /** День 7–18, вечер 18–22, ночь 22–5; на рассвете (5–7) — тёплая «вечерняя» картинка. */
        fun forHour(hour: Int): TimeOfDay = when (hour) {
            in 7 until 18 -> DAY
            in 18 until 22, in 5 until 7 -> EVENING
            else -> NIGHT
        }

        fun now(): TimeOfDay = forHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    }
}

val LocalTimeOfDay = staticCompositionLocalOf { TimeOfDay.DAY }
