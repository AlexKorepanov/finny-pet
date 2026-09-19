package ru.finny.petgame.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import ru.finny.petgame.data.entity.ProfileEntity
import ru.finny.petgame.data.repository.GameRepository
import ru.finny.petgame.ui.components.HintDialog
import ru.finny.petgame.ui.screens.IntroScreen
import ru.finny.petgame.ui.screens.MainStubScreen
import ru.finny.petgame.ui.screens.PetConfirmScreen
import ru.finny.petgame.ui.screens.PetScreen
import ru.finny.petgame.ui.screens.ProfileScreen

enum class AppScreen {
    LOADING,
    INTRO,
    PROFILE,
    PET,
    PET_CONFIRM,
    MAIN,
}

@Composable
fun PetGameApp(repository: GameRepository) {
    var screen by remember { mutableStateOf(AppScreen.LOADING) }
    var introPage by rememberSaveable { mutableIntStateOf(0) }
    var playerName by rememberSaveable { mutableStateOf("") }
    var speciesIndex by rememberSaveable { mutableIntStateOf(0) }
    var colorIndex by rememberSaveable { mutableIntStateOf(0) }
    var petName by rememberSaveable { mutableStateOf("") }
    var showHint by remember { mutableStateOf(false) }
    var savedProfile by remember { mutableStateOf<ProfileEntity?>(null) }

    LaunchedEffect(Unit) {
        val snapshot = repository.loadSnapshot()
        savedProfile = snapshot?.profile
        screen = if (snapshot != null) AppScreen.MAIN else AppScreen.INTRO
    }

    val goBack: () -> Unit = {
        when (screen) {
            AppScreen.INTRO -> if (introPage > 0) introPage--
            AppScreen.PROFILE -> {
                introPage = 1
                screen = AppScreen.INTRO
            }
            AppScreen.PET -> screen = AppScreen.PROFILE
            AppScreen.PET_CONFIRM -> screen = AppScreen.PET
            else -> {}
        }
    }

    BackHandler(
        enabled = screen != AppScreen.LOADING &&
            screen != AppScreen.MAIN &&
            !(screen == AppScreen.INTRO && introPage == 0),
    ) {
        goBack()
    }

    val scope = rememberCoroutineScope()

    when (screen) {
        AppScreen.LOADING -> LoadingScreen()
        AppScreen.INTRO -> IntroScreen(
            page = introPage,
            onBack = goBack,
            onHint = { showHint = true },
            onNextPage = { introPage = 1 },
            onFinish = { screen = AppScreen.PROFILE },
        )
        AppScreen.PROFILE -> ProfileScreen(
            playerName = playerName,
            speciesIndex = speciesIndex,
            onPlayerNameChange = { playerName = it },
            onSpeciesChange = { speciesIndex = it },
            onBack = goBack,
            onHint = { showHint = true },
            onNext = { screen = AppScreen.PET },
        )
        AppScreen.PET -> PetScreen(
            speciesIndex = speciesIndex,
            colorIndex = colorIndex,
            petName = petName,
            onSpeciesChange = { speciesIndex = it },
            onColorChange = { colorIndex = it },
            onPetNameChange = { petName = it },
            onBack = goBack,
            onHint = { showHint = true },
            onNext = { screen = AppScreen.PET_CONFIRM },
        )
        AppScreen.PET_CONFIRM -> PetConfirmScreen(
            playerName = playerName.trim(),
            petName = petName.trim(),
            speciesIndex = speciesIndex,
            colorIndex = colorIndex,
            onBack = goBack,
            onHint = { showHint = true },
            onSave = {
                scope.launch {
                    repository.createProfile(
                        playerName = playerName.trim(),
                        petName = petName.trim(),
                        petSpecies = speciesIndex,
                        petColor = colorIndex,
                    )
                    savedProfile = repository.loadSnapshot()?.profile
                    screen = AppScreen.MAIN
                }
            },
        )
        AppScreen.MAIN -> MainStubScreen(
            profile = savedProfile,
            onHint = { showHint = true },
        )
    }

    if (showHint) {
        HintDialog(onClose = { showHint = false })
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}