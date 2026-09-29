package ru.finny.petgame.feedback

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import ru.finny.petgame.ui.components.LeafPhase
import ru.finny.petgame.ui.components.leafRustle
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Вибрация в такт листопаду после заставки. Узор строится из движения самих листьев
 * ([leafRustle]): пока листья летят быстро и их много — дрожь сильнее, оседают — затихает.
 * Когда занавес сомкнулся — толчок, когда раскрывается — хлопок и затихающий шелест.
 *
 * Узор запускается одновременно с анимацией и длится ровно столько же.
 *
 * Это игровой эффект, а не отклик на касание, поэтому он идёт как медиа-вибрация:
 * системный «Виброотклик» (на Samsung часто выключен) его не глушит,
 * а выключается он своим переключателем в настройках игры.
 */
class LeafHaptics(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var enabled by mutableStateOf(prefs.getBoolean(KEY_ENABLED, true))
        private set

    fun updateEnabled(value: Boolean) {
        enabled = value
        prefs.edit { putBoolean(KEY_ENABLED, value) }
        if (!value) cancel()
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    /**
     * Вибрирует, пока идёт фаза листопада, сверяясь с [progress] анимации каждые [SEGMENT_MS]:
     * мотор может исполнять узор медленнее, чем задано, и без сверки дрожь отставала бы от листьев.
     */
    suspend fun follow(phase: LeafPhase, durationMs: Int, progress: () -> Float) {
        if (!enabled) return
        val curve = buildCurve(phase, durationMs)
        val segmentSteps = SEGMENT_MS / STEP_MS
        while (true) {
            val p = progress()
            if (p >= 1f) break
            val from = (p * curve.size).toInt().coerceIn(0, curve.lastIndex)
            // На шаг длиннее паузы, чтобы между кусочками не было провалов.
            val to = minOf(from + segmentSteps + 1, curve.size)
            play(curve.copyOfRange(from, to))
            delay(SEGMENT_MS.toLong())
        }
    }

    fun cancel() {
        vibrator?.cancel()
    }

    /** Сила вибрации 0..1 для каждого отрезка по [STEP_MS]. */
    private fun buildCurve(phase: LeafPhase, durationMs: Int): FloatArray {
        val steps = (durationMs / STEP_MS).coerceAtLeast(1)
        val rustle = leafRustle(phase, steps)
        return FloatArray(steps) { i ->
            val p = (i + 0.5f) / steps
            // Мелкая «зернистость», чтобы ощущалось как шорох, а не ровный гул.
            val grain = if (i % 2 == 0) 1f else 0.65f
            val rustleLevel = RUSTLE_LEVEL * rustle[i].pow(0.8f) * grain
            val accent = when (phase) {
                // Занавес становится сплошным к 90% — в этот момент и толчок.
                LeafPhase.COVER -> pulse(p, center = 0.9f, width = 0.07f)
                // Занавес распахивается в самом начале раскрытия.
                LeafPhase.REVEAL -> pulse(p, center = 0.02f, width = 0.06f) * 0.9f
            }
            maxOf(rustleLevel, accent).coerceIn(0f, 1f)
        }
    }

    private fun pulse(p: Float, center: Float, width: Float): Float {
        val d = (p - center) / width
        return if (d in -1f..1f) 1f - d * d else 0f
    }

    private fun play(curve: FloatArray) {
        val vibrator = vibrator ?: return
        if (!enabled || !vibrator.hasVibrator()) return
        val effect = if (vibrator.hasAmplitudeControl()) {
            amplitudeWaveform(curve)
        } else {
            onOffWaveform(curve)
        } ?: return
        vibrator.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                effect,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
    }

    /**
     * Соседние отрезки одной силы склеиваются в один шаг: на Samsung каждый шаг
     * узора исполняется с накладными расходами, и из сотни мелких шагов
     * вибрация растягивалась вдвое и отставала от листьев.
     */
    private fun amplitudeWaveform(curve: FloatArray): VibrationEffect? {
        val timings = mutableListOf<Long>()
        val amplitudes = mutableListOf<Int>()
        for (level in curve) {
            val amplitude = if (level < MIN_LEVEL) {
                0
            } else {
                ((level * AMPLITUDE_LEVELS).roundToInt() * 255 / AMPLITUDE_LEVELS).coerceIn(1, 255)
            }
            if (amplitudes.lastOrNull() == amplitude) {
                timings[timings.lastIndex] += STEP_MS.toLong()
            } else {
                timings += STEP_MS.toLong()
                amplitudes += amplitude
            }
        }
        if (amplitudes.none { it > 0 }) return null
        return VibrationEffect.createWaveform(timings.toLongArray(), amplitudes.toIntArray(), -1)
    }

    /** Мотор без регулировки силы: силу передаём долей «включено» в каждом окне. */
    private fun onOffWaveform(curve: FloatArray): VibrationEffect? {
        val stepsPerWindow = PWM_WINDOW_MS / STEP_MS
        val timings = mutableListOf<Long>()
        for (start in curve.indices step stepsPerWindow) {
            val end = minOf(start + stepsPerWindow, curve.size)
            val level = (start until end).maxOf { curve[it] }
            val windowMs = (end - start) * STEP_MS
            val on = if (level < MIN_LEVEL) 0 else (level * windowMs).roundToInt().coerceAtLeast(STEP_MS)
            timings += (windowMs - on).toLong()
            timings += on.toLong()
        }
        if (timings.none { it > 0 }) return null
        return VibrationEffect.createWaveform(timings.toLongArray(), -1)
    }

    private companion object {
        const val PREFS = "finny_settings"
        const val KEY_ENABLED = "vibration_enabled"
        const val STEP_MS = 20
        const val SEGMENT_MS = 80
        const val AMPLITUDE_LEVELS = 12
        const val PWM_WINDOW_MS = 40
        const val RUSTLE_LEVEL = 0.6f
        const val MIN_LEVEL = 0.08f
    }
}
