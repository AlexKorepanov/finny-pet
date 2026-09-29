package ru.finny.petgame.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import ru.finny.petgame.R

enum class SoundEffect { CORRECT, WRONG, LESSON_DONE }

/** Короткие звуки заданий. Выключаются отдельно от музыки; смысл всегда дублируется на экране. */
class SoundEffects(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var pool: SoundPool? = null
    private val ids = mutableMapOf<SoundEffect, Int>()

    var enabled by mutableStateOf(prefs.getBoolean(KEY_ENABLED, true))
        private set

    fun updateEnabled(value: Boolean) {
        enabled = value
        prefs.edit { putBoolean(KEY_ENABLED, value) }
    }

    fun play(effect: SoundEffect) {
        if (!enabled) return
        val soundPool = pool ?: createPool().also { pool = it }
        val id = ids[effect] ?: return
        soundPool.play(id, VOLUME, VOLUME, 1, 0, 1f)
    }

    fun release() {
        pool?.release()
        pool = null
        ids.clear()
    }

    private fun createPool(): SoundPool {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val soundPool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attributes).build()
        ids[SoundEffect.CORRECT] = soundPool.load(appContext, R.raw.sfx_correct, 1)
        ids[SoundEffect.WRONG] = soundPool.load(appContext, R.raw.sfx_wrong, 1)
        ids[SoundEffect.LESSON_DONE] = soundPool.load(appContext, R.raw.sfx_lesson_done, 1)
        return soundPool
    }

    /** Звуки нужны сразу после первого ответа, поэтому загружаем их заранее. */
    fun preload() {
        if (pool == null) pool = createPool()
    }

    private companion object {
        const val PREFS = "finny_settings"
        const val KEY_ENABLED = "sounds_enabled"
        const val VOLUME = 0.8f
    }
}
