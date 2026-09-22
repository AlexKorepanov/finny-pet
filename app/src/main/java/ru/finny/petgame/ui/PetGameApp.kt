package ru.finny.petgame.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.finny.petgame.R
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.data.model.PeriodStatus
import ru.finny.petgame.data.repository.GameRepository
import ru.finny.petgame.ui.components.HintDialog
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SectionAccent
import ru.finny.petgame.ui.components.SecondaryButton
import ru.finny.petgame.ui.screens.IntroScreen
import ru.finny.petgame.ui.screens.MainScreen
import ru.finny.petgame.ui.screens.PetConfirmScreen
import ru.finny.petgame.ui.screens.PetScreen
import ru.finny.petgame.ui.screens.PlanScreen
import ru.finny.petgame.ui.screens.ProfileScreen
import ru.finny.petgame.ui.screens.SavingsScreen
import ru.finny.petgame.ui.screens.SectionStubScreen
import ru.finny.petgame.ui.screens.ShopScreen
import ru.finny.petgame.ui.screens.TasksScreen

enum class AppScreen {
    LOADING,
    INTRO,
    PROFILE,
    PET,
    PET_CONFIRM,
    MAIN,
    PLAN,
    SHOP,
    SAVINGS,
    TASKS,
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
    var showPlanHint by remember { mutableStateOf(false) }
    var focusedTaskId by rememberSaveable { mutableStateOf<String?>(null) }
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
        if ((screen == AppScreen.SHOP || screen == AppScreen.SAVINGS) && !isPlanConfirmed(mainSnapshot)) {
            showPlanHint = true
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
            AppScreen.SHOP -> screen = AppScreen.MAIN
            AppScreen.SAVINGS -> screen = AppScreen.MAIN
            AppScreen.TASKS -> screen = AppScreen.MAIN
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
            onShop = { screen = AppScreen.SHOP },
            onSavings = { screen = AppScreen.SAVINGS },
            onSection = { titleRes ->
                sectionTitleRes = titleRes
                screen = AppScreen.SECTION_STUB
            },
            onOpenTask = { taskId ->
                focusedTaskId = taskId
                screen = AppScreen.TASKS
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
        AppScreen.SHOP -> {
            val currentSnapshot = mainSnapshot
            if (currentSnapshot == null) {
                LoadingScreen()
            } else {
                ShopScreen(
                    repository = repository,
                    snapshot = currentSnapshot,
                    onBack = goBack,
                    onHint = { showHint = true },
                    onShopChanged = {
                        scope.launch { mainSnapshot = repository.loadSnapshot() }
                    },
                )
            }
        }
        AppScreen.SAVINGS -> {
            val currentSnapshot = mainSnapshot
            if (currentSnapshot == null) {
                LoadingScreen()
            } else {
                SavingsScreen(
                    repository = repository,
                    snapshot = currentSnapshot,
                    onBack = goBack,
                    onHint = { showHint = true },
                    onSavingsChanged = {
                        scope.launch { mainSnapshot = repository.loadSnapshot() }
                    },
                )
            }
        }
        AppScreen.TASKS -> {
            val currentSnapshot = mainSnapshot
            if (currentSnapshot == null) {
                LoadingScreen()
            } else {
                TasksScreen(
                    repository = repository,
                    snapshot = currentSnapshot,
                    focusedTaskId = focusedTaskId,
                    onBack = goBack,
                    onHint = { showHint = true },
                    onTasksChanged = {
                        scope.launch { mainSnapshot = repository.loadSnapshot() }
                    },
                    onOpenPlan = { screen = AppScreen.PLAN },
                    onOpenShop = { screen = AppScreen.SHOP },
                )
            }
        }
        AppScreen.SECTION_STUB -> SectionStubScreen(
            title = stringResource(sectionTitleRes),
            accent = sectionAccentFor(sectionTitleRes),
            petSpecies = mainSnapshot?.profile?.petSpecies ?: 0,
            petColorIndex = mainSnapshot?.profile?.petColor ?: 0,
            onBack = goBack,
            onHint = { showHint = true },
        )
    }

    if (showHint) {
        HintDialog(onClose = { showHint = false })
    }

    if (showPlanHint) {
        AlertDialog(
            onDismissRequest = { showPlanHint = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(text = stringResource(R.string.plan_first_hint_title)) },
            text = { Text(text = stringResource(R.string.plan_first_hint_text)) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.plan_make),
                        onClick = {
                            showPlanHint = false
                            screen = AppScreen.PLAN
                        },
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = stringResource(R.string.plan_skip),
                        onClick = { showPlanHint = false },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private fun sectionAccentFor(titleRes: Int): SectionAccent = when (titleRes) {
    R.string.section_plan -> SectionAccent.PLAN
    R.string.section_shop -> SectionAccent.SHOP
    R.string.section_tasks -> SectionAccent.TASKS
    R.string.section_savings -> SectionAccent.SAVINGS
    R.string.section_progress -> SectionAccent.PROGRESS
    else -> SectionAccent.ADULT
}

private fun isPlanConfirmed(snapshot: GameSnapshot?): Boolean =
    snapshot?.currentPeriod?.status == PeriodStatus.ACTIVE.name ||
        snapshot?.currentPeriod?.status == PeriodStatus.CLOSED.name