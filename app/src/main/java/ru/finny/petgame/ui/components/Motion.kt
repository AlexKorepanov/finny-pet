package ru.finny.petgame.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

/** false — анимации выключены в настройках игры или в системе: всё показывается сразу и неподвижно. */
val LocalAnimationsEnabled = staticCompositionLocalOf { true }
