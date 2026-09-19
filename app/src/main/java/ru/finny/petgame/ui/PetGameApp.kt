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
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import ru.finny.petgame.R
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.data.repository.GameRepository
import ru.finny.petgame.ui.components.HintDialog
import ru.finny.petgame.ui.screens.IntroScreen
import ru.finny.petgame.ui.screens.MainScreen
import ru.finny.petgame.ui.screens.PetConfirmScreen
import ru.finny.petgame.ui.screens.PetScreen
import ru.finny.petgame.ui.screens.PlanScreen
import ru.finny.petgame.ui.screens.ProfileScreen
import ru.finny.petgame.ui.screens.SectionStubScreen

enum class AppScreen {
    LOADING,
    INTRO,
    PROFILE,
    PET,
    PET_CONFIRM,
    MAIN,
    PLAN,
    SECTION_STUB,
}

@Composable
fun PetGameApp(repository: GameRepository) {
    var screen by remember { mutableStateOf(AppScreen.LOADING) }
    var introPage by rememberSaveable { mutableIntStateOf(0) }
    var playerName by rememberSaveable { mutableStateOf("") }
    var avatarIndex by rememberSaveable { mutableIntStateOf(0) }
    var speciesIndex by rememberSaveable { mutableIntStateOf(0) }
    var colorIndex by rememberSaveable { mutableIntStateOf(0) }
    var petName by rememberSaveable { mutableStateOf("") }
    var sectionTitleRes by rememberSaveable { mutableIntStateOf(R.string.section_plan) }
    var showHint by remember { mutableStateOf(false) }
    var mainSnapshot by remember { mutableStateOf<GameSnapshot?>(null) }

    LaunchedEffect(Unit) {
        val snapshot = repository.loadSnapshot()
        mainSnapshot = snapshot
        screen = if (snapshot != null) AppScreen.MAIN else AppScreen.INTRO
    }

    LaunchedEffect(screen) {
        if (screen == AppScreen.MAIN) {
            mainSnapshot = repository.loadSnapshot()
        }
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
            AppScreen.PLAN -> screen = AppScreen.MAIN
            AppScreen.SECTION_STUB -> screen = AppScreen.MAIN
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
            avatarIndex = avatarIndex,
            onPlayerNameChange = { playerName = it },
            onAvatarChange = { avatarIndex = it },
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
                        playerAvatar = avatarIndex,
                    )
                    mainSnapshot = repository.loadSnapshot()
                    screen = AppScreen.MAIN
                }
            },
        )
        AppScreen.MAIN -> MainScreen(
            snapshot = mainSnapshot,
            onHint = { showHint = true },
            onPlan = { screen = AppScreen.PLAN },
            onSection = { titleRes ->
                sectionTitleRes = titleRes
                screen = AppScreen.SECTION_STUB
            },
        )
        AppScreen.PLAN -> {
            val currentSnapshot = mainSnapshot
            if (currentSnapshot == null) {
                LoadingScreen()
            } else {
                PlanScreen(
                    repository = repository,
                    snapshot = currentSnapshot,
                    onBack = goBack,
                    onHint = { showHint = true },
                    onPlanConfirmed = {
                        scope.launch { mainSnapshot = repository.loadSnapshot() }
                    },
                )
            }
        }
        AppScreen.SECTION_STUB -> SectionStubScreen(
            title = stringResource(sectionTitleRes),
            onBack = goBack,
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