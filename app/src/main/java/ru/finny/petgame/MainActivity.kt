package ru.finny.petgame

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ru.finny.petgame.audio.BackgroundMusic
import ru.finny.petgame.audio.SoundEffects
import ru.finny.petgame.data.repository.GameRepository
import ru.finny.petgame.economy.EconomyEngine
import ru.finny.petgame.settings.UiSettings
import ru.finny.petgame.ui.components.LocalAnimationsEnabled
import ru.finny.petgame.ui.PetGameApp
import ru.finny.petgame.ui.theme.FinnyColors
import ru.finny.petgame.ui.theme.FinnyPetTheme
import ru.finny.petgame.ui.theme.LocalTimeOfDay
import ru.finny.petgame.ui.theme.TimeOfDay

class MainActivity : ComponentActivity() {
    private lateinit var music: BackgroundMusic
    private lateinit var sounds: SoundEffects
    private lateinit var uiSettings: UiSettings
    private var timeOfDay by mutableStateOf(TimeOfDay.now())

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        var contentReady by mutableStateOf(false)
        splash.setKeepOnScreenCondition { !contentReady }
        val repository = GameRepository(
            database = (application as PetGameApplication).database,
            engine = EconomyEngine(),
        )
        music = BackgroundMusic(this)
        sounds = SoundEffects(this).apply { preload() }
        uiSettings = UiSettings(this)
        FinnyColors.isDark = { uiSettings.darkTheme }
        timeOfDay = currentTimeOfDay()
        window.setBackgroundDrawableResource(timeOfDay.splash)
        setContent {
            SideEffect { contentReady = true }
            val dark = uiSettings.darkTheme
            LaunchedEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                val bars = if (dark) {
                    SystemBarStyle.dark(transparent)
                } else {
                    SystemBarStyle.light(transparent, transparent)
                }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            FinnyPetTheme(dark = dark) {
                CompositionLocalProvider(
                    LocalAnimationsEnabled provides uiSettings.animationsActive,
                    LocalTimeOfDay provides timeOfDay,
                ) {
                    PetGameApp(repository = repository, music = music, sounds = sounds, uiSettings = uiSettings)
                }
            }
        }
    }

    /** В отладочной сборке время суток можно задать при запуске: `am start ... --ei debug_hour 21`. */
    private fun currentTimeOfDay(): TimeOfDay {
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val hour = intent?.getIntExtra(EXTRA_DEBUG_HOUR, -1) ?: -1
        return if (debuggable && hour in 0..23) TimeOfDay.forHour(hour) else TimeOfDay.now()
    }

    private companion object {
        const val EXTRA_DEBUG_HOUR = "debug_hour"
    }

    override fun onStart() {
        super.onStart()
        uiSettings.refreshSystemState()
        timeOfDay = currentTimeOfDay()
        music.onStart()
    }

    override fun onStop() {
        music.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        music.release()
        sounds.release()
        super.onDestroy()
    }
}
