package ru.finny.petgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.finny.petgame.data.repository.GameRepository
import ru.finny.petgame.economy.EconomyEngine
import ru.finny.petgame.ui.PetGameApp
import ru.finny.petgame.ui.theme.FinnyPetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = GameRepository(
            database = (application as PetGameApplication).database,
            engine = EconomyEngine(),
        )
        setContent {
            FinnyPetTheme {
                PetGameApp(repository)
            }
        }
    }
}