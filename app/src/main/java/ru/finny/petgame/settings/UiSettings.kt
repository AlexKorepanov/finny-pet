package ru.finny.petgame.settings

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/** Настройки интерфейса на этом устройстве: анимации и какие цели уже «появились» на полянке. */
class UiSettings(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var animationsEnabled by mutableStateOf(prefs.getBoolean(KEY_ANIMATIONS, true))
        private set

    /** «Удалить анимации» в специальных возможностях Android. */
    var systemAnimationsOff by mutableStateOf(readSystemAnimationsOff())
        private set

    val animationsActive: Boolean
        get() = animationsEnabled && !systemAnimationsOff

    var seenSceneGoals by mutableStateOf(prefs.getStringSet(KEY_SEEN_GOALS, emptySet()).orEmpty().toSet())
        private set

    fun updateAnimationsEnabled(value: Boolean) {
        animationsEnabled = value
        prefs.edit { putBoolean(KEY_ANIMATIONS, value) }
    }

    fun refreshSystemState() {
        systemAnimationsOff = readSystemAnimationsOff()
    }

    fun markSceneGoalsSeen(goalIds: Set<String>) {
        if (seenSceneGoals.containsAll(goalIds)) return
        seenSceneGoals = seenSceneGoals + goalIds
        prefs.edit { putStringSet(KEY_SEEN_GOALS, seenSceneGoals) }
    }

    fun clearSceneGoalsSeen() {
        seenSceneGoals = emptySet()
        prefs.edit { remove(KEY_SEEN_GOALS) }
    }

    private fun readSystemAnimationsOff(): Boolean =
        Settings.Global.getFloat(appContext.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

    private companion object {
        const val PREFS = "finny_settings"
        const val KEY_ANIMATIONS = "animations_enabled"
        const val KEY_SEEN_GOALS = "scene_seen_goals"
    }
}
