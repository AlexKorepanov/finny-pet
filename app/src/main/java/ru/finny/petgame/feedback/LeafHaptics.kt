package ru.finny.petgame.feedback

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import kotlinx.coroutines.delay

/**
 * Лёгкая вибрация во время листопада после заставки:
 * листья налетают — тики всё чаще, закрыли экран — мягкий толчок,
 * разлетаются — тики всё реже.
 *
 * Работает через системный тактильный отклик [View.performHapticFeedback]:
 * разрешение VIBRATE не нужно, а системная настройка «Виброотклик» соблюдается.
 */
class LeafHaptics(private val view: View) {
    private val tick = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
    } else {
        HapticFeedbackConstants.CLOCK_TICK
    }
    private val thud = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
    } else {
        HapticFeedbackConstants.VIRTUAL_KEY
    }
    private val release = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE
    } else {
        HapticFeedbackConstants.CLOCK_TICK
    }

    suspend fun cover(durationMs: Int) {
        playTicks(durationMs, faster = true)
    }

    fun covered() {
        view.performHapticFeedback(thud)
    }

    suspend fun reveal(durationMs: Int) {
        view.performHapticFeedback(release)
        playTicks(durationMs * 2 / 3, faster = false)
    }

    /** Паузы между тиками растут или сокращаются, так дробь «нарастает» или «затихает». */
    private suspend fun playTicks(durationMs: Int, faster: Boolean) {
        val weights = if (faster) COVER_WEIGHTS else REVEAL_WEIGHTS
        val unit = durationMs.toFloat() / weights.sum()
        for (weight in weights) {
            delay((weight * unit).toLong())
            view.performHapticFeedback(tick)
        }
    }

    private companion object {
        val COVER_WEIGHTS = intArrayOf(5, 4, 4, 3, 3, 2, 2)
        val REVEAL_WEIGHTS = intArrayOf(2, 3, 4, 5, 6)
    }
}
