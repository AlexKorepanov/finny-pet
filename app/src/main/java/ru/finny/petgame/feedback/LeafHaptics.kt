package ru.finny.petgame.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Лёгкая вибрация во время листопада после заставки:
 * листья налетают — нарастающая «шуршащая» дробь, закрыли экран — мягкий толчок,
 * разлетаются — затихающая дробь.
 */
class LeafHaptics(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun cover(durationMs: Int) {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        val ticks = 7
        val tick = VibrationEffect.Composition.PRIMITIVE_LOW_TICK
        val step = pause(v, durationMs, tick, ticks)
        val effect = composition(v) { c ->
            for (i in 0 until ticks) {
                c.addPrimitive(tick, 0.15f + 0.07f * i, if (i == 0) 0 else step)
            }
        } ?: waveform(v, durationMs, rising = true)
        play(v, effect)
    }

    /** Мягкий толчок в момент, когда листья полностью закрыли экран. */
    fun covered() {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        val thud = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            v.arePrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD).all { it }
        ) {
            VibrationEffect.Composition.PRIMITIVE_THUD
        } else {
            VibrationEffect.Composition.PRIMITIVE_CLICK
        }
        val effect = composition(v) { it.addPrimitive(thud, 0.7f) }
            ?: VibrationEffect.createOneShot(30L, if (v.hasAmplitudeControl()) 160 else VibrationEffect.DEFAULT_AMPLITUDE)
        play(v, effect)
    }

    fun reveal(durationMs: Int) {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        val ticks = 5
        val tick = VibrationEffect.Composition.PRIMITIVE_LOW_TICK
        val fall = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            v.arePrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL).all { it }
        ) {
            VibrationEffect.Composition.PRIMITIVE_QUICK_FALL
        } else {
            VibrationEffect.Composition.PRIMITIVE_TICK
        }
        val step = pause(v, durationMs * 2 / 3, tick, ticks)
        val effect = composition(v) { c ->
            c.addPrimitive(fall, 0.5f)
            for (i in 0 until ticks) {
                c.addPrimitive(tick, 0.5f - 0.09f * i, step)
            }
        } ?: waveform(v, durationMs / 2, rising = false)
        play(v, effect)
    }

    /** Пауза между тиками, чтобы весь эффект уложился в [durationMs] с учётом длины импульсов на этом телефоне. */
    private fun pause(v: Vibrator, durationMs: Int, tick: Int, ticks: Int): Int {
        val tickMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) v.getPrimitiveDurations(tick)[0] else 12
        return ((durationMs - ticks * tickMs) / ticks).coerceAtLeast(0)
    }

    private fun composition(
        v: Vibrator,
        build: (VibrationEffect.Composition) -> Unit,
    ): VibrationEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val needed = intArrayOf(
            VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
            VibrationEffect.Composition.PRIMITIVE_TICK,
            VibrationEffect.Composition.PRIMITIVE_CLICK,
        )
        if (!v.areAllPrimitivesSupported(*needed)) return null
        return VibrationEffect.startComposition().also(build).compose()
    }

    /** Для телефонов без «примитивов»: короткие импульсы с нарастающей или затихающей силой. */
    private fun waveform(v: Vibrator, durationMs: Int, rising: Boolean): VibrationEffect {
        if (!v.hasAmplitudeControl()) {
            return VibrationEffect.createOneShot(25L, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        val pulses = 6
        val gap = (durationMs / pulses - 12).coerceAtLeast(10).toLong()
        val timings = LongArray(pulses * 2) { if (it % 2 == 0) gap else 12L }
        timings[0] = 0L
        val amplitudes = IntArray(pulses * 2) { i ->
            if (i % 2 == 0) {
                0
            } else {
                val k = i / 2
                val level = if (rising) k else pulses - 1 - k
                40 + level * 30
            }
        }
        return VibrationEffect.createWaveform(timings, amplitudes, -1)
    }

    private fun play(v: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        } else {
            v.vibrate(effect)
        }
    }
}
