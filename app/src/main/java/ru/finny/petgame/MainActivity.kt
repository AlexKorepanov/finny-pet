package ru.finny.petgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ru.finny.petgame.audio.BackgroundMusic
import ru.finny.petgame.data.repository.GameRepository
import ru.finny.petgame.economy.EconomyEngine
import ru.finny.petgame.settings.UiSettings
import ru.finny.petgame.ui.components.LocalAnimationsEnabled
import ru.finny.petgame.ui.PetGameApp
import ru.finny.petgame.ui.theme.FinnyPetTheme

class MainActivity : ComponentActivity() {
    private lateinit var music: BackgroundMusic
    private lateinit var uiSettings: UiSettings

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
        uiSettings = UiSettings(this)
        setContent {
            SideEffect { contentReady = true }
            FinnyPetTheme {
                CompositionLocalProvider(LocalAnimationsEnabled provides uiSettings.animationsActive) {
                    PetGameApp(repository = repository, music = music, uiSettings = uiSettings)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        uiSettings.refreshSystemState()
        music.onStart()
    }

    override fun onStop() {
        music.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        music.release()
        super.onDestroy()
    }
}
