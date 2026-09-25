package ru.finny.petgame.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import ru.finny.petgame.R

/** Тихая фоновая мелодия: играет, только пока приложение на экране и музыка не выключена. */
class BackgroundMusic(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var player: MediaPlayer? = null
    private var inForeground = false

    var enabled by mutableStateOf(prefs.getBoolean(KEY_ENABLED, true))
        private set

    /** Громкость с ползунка, 0..1. */
    var volume by mutableFloatStateOf(prefs.getFloat(KEY_VOLUME, DEFAULT_VOLUME))
        private set

    fun updateEnabled(value: Boolean) {
        enabled = value
        prefs.edit { putBoolean(KEY_ENABLED, value) }
        sync()
    }

    fun updateVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        applyVolume()
    }

    /** Сохраняет громкость, когда ползунок отпущен, чтобы не писать настройки на каждый кадр. */
    fun saveVolume() {
        prefs.edit { putFloat(KEY_VOLUME, volume) }
    }

    fun onStart() {
        inForeground = true
        sync()
    }

    fun onStop() {
        inForeground = false
        sync()
    }

    fun release() {
        player?.release()
        player = null
    }

    private fun sync() {
        if (enabled && inForeground) {
            val current = player ?: createPlayer()?.also { player = it } ?: return
            if (!current.isPlaying) current.start()
        } else {
            player?.takeIf { it.isPlaying }?.pause()
        }
    }

    private fun applyVolume() {
        // Квадрат ближе к тому, как ухо слышит изменение громкости.
        val gain = volume * volume
        player?.setVolume(gain, gain)
    }

    private fun createPlayer(): MediaPlayer? {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        return MediaPlayer.create(appContext, R.raw.music_forest, attributes, AudioManager.AUDIO_SESSION_ID_GENERATE)
            ?.apply { isLooping = true }
            ?.also {
                player = it
                applyVolume()
            }
    }

    private companion object {
        const val PREFS = "finny_settings"
        const val KEY_ENABLED = "music_enabled"
        const val KEY_VOLUME = "music_volume"
        const val DEFAULT_VOLUME = 0.6f
    }
}
