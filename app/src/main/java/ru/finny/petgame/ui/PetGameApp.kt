package ru.finny.petgame.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.finny.petgame.R
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.data.model.PeriodCloseResult
import ru.finny.petgame.data.model.PeriodStatus
import ru.finny.petgame.data.repository.GameRepository
import ru.finny.petgame.ui.components.HintDialog
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SecondaryButton
import ru.finny.petgame.ui.model.PetLook
import ru.finny.petgame.ui.model.toPetLook
import ru.finny.petgame.ui.screens.AdultScreen
import ru.finny.petgame.ui.screens.IntroScreen
import ru.finny.petgame.ui.screens.MainScreen
import ru.finny.petgame.ui.screens.PeriodResultScreen
import ru.finny.petgame.ui.screens.PetConfirmScreen
import ru.finny.petgame.ui.screens.PetScreen
import ru.finny.petgame.ui.screens.PlanScreen
import ru.finny.petgame.ui.screens.ProgressScreen
import ru.finny.petgame.ui.screens.SavingsScreen
import ru.finny.petgame.ui.screens.ShopScreen
import ru.finny.petgame.ui.screens.TasksScreen

private val PetLookSaver = Saver<PetLook, List<Int>>(
    save = { listOf(it.hat, it.face, it.outfit, it.emotion, it.eyeColor) },
    restore = { PetLook(it[0], it[1], it[2], it[3], it[4]) },
)

private const val DEFAULT_PLAYER_NAME = "Друг"
private const val SPLASH_MIN_MS = 1_800L

enum class AppScreen {
    LOADING,
    INTRO,
    PET,
    PET_CONFIRM,
    MAIN,
    PLAN,
    SHOP,
    SAVINGS,
    TASKS,
    PROGRESS,
    ADULT,
    PERIOD_RESULT,
}

@Composable
fun PetGameApp(repository: GameRepository) {
    var screen by remember { mutableStateOf(AppScreen.LOADING) }
    var introPage by rememberSaveable { mutableIntStateOf(0) }
    var petLook by rememberSaveable(stateSaver = PetLookSaver) { mutableStateOf(PetLook.Default) }
    var petName by rememberSaveable { mutableStateOf("") }
    var showHint by remember { mutableStateOf(false) }
    var showPlanHint by remember { mutableStateOf(false) }
    var showClosePeriodConfirm by remember { mutableStateOf(false) }
    var focusedTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var mainSnapshot by remember { mutableStateOf<GameSnapshot?>(null) }
    var periodCloseResult by remember { mutableStateOf<PeriodCloseResult.Success?>(null) }

    LaunchedEffect(Unit) {
        val startedAt = System.currentTimeMillis()
        val snapshot = repository.loadSnapshot()
        mainSnapshot = snapshot
        val elapsed = System.currentTimeMillis() - startedAt
        if (elapsed < SPLASH_MIN_MS) {
            delay(SPLASH_MIN_MS - elapsed)
        }
        screen = if (snapshot != null) AppScreen.MAIN else AppScreen.INTRO
    }

    LaunchedEffect(screen) {
        if (screen == AppScreen.MAIN ||
            screen == AppScreen.PROGRESS ||
            screen == AppScreen.ADULT
        ) {
            mainSnapshot = repository.loadSnapshot()
        }
        if ((screen == AppScreen.SHOP || screen == AppScreen.SAVINGS) && !isPlanConfirmed(mainSnapshot)) {
            showPlanHint = true
        }
    }

    val goBack: () -> Unit = {
        when (screen) {
            AppScreen.INTRO -> if (introPage > 0) introPage--
            AppScreen.PET -> {
                introPage = 1
                screen = AppScreen.INTRO
            }
            AppScreen.PET_CONFIRM -> screen = AppScreen.PET
            AppScreen.PLAN -> screen = AppScreen.MAIN
            AppScreen.SHOP -> screen = AppScreen.MAIN
            AppScreen.SAVINGS -> screen = AppScreen.MAIN
            AppScreen.TASKS -> screen = AppScreen.MAIN
            AppScreen.PROGRESS -> screen = AppScreen.MAIN
            AppScreen.ADULT -> screen = AppScreen.MAIN
            AppScreen.PERIOD_RESULT -> screen = AppScreen.MAIN
            else -> {}
        }
    }

    BackHandler(
        enabled = screen != AppScreen.LOADING &&
            screen != AppScreen.MAIN &&
            screen != AppScreen.PERIOD_RESULT &&
            !(screen == AppScreen.INTRO && introPage == 0),
    ) {
        goBack()
    }

    val scope = rememberCoroutineScope()
    val requestClosePeriod: () -> Unit = { showClosePeriodConfirm = true }
    val performClosePeriod: () -> Unit = {
        scope.launch {
            when (val result = repository.closePeriod()) {
                is PeriodCloseResult.Success -> {
                    periodCloseResult = result
                    mainSnapshot = repository.loadSnapshot()
                    showClosePeriodConfirm = false
                    screen = AppScreen.PERIOD_RESULT
                }
                is PeriodCloseResult.Invalid -> {
                    showClosePeriodConfirm = false
                }
            }
        }
    }

    when (screen) {
        AppScreen.LOADING -> LaunchSplashScreen()
        AppScreen.INTRO -> IntroScreen(
            page = introPage,
            onBack = goBack,
            onHint = { showHint = true },
            onNextPage = { introPage = 1 },
            onFinish = { screen = AppScreen.PET },
        )
        AppScreen.PET -> PetScreen(
            look = petLook,
            petName = petName,
            onLookChange = { petLook = it },
            onPetNameChange = { petName = it },
            onBack = goBack,
            onHint = { showHint = true },
            onNext = { screen = AppScreen.PET_CONFIRM },
        )
        AppScreen.PET_CONFIRM -> PetConfirmScreen(
            petName = petName.trim(),
            look = petLook,
            onBack = goBack,
            onHint = { showHint = true },
            onSave = {
                scope.launch {
                    repository.createProfile(
                        playerName = DEFAULT_PLAYER_NAME,
                        petName = petName.trim(),
                        petHat = petLook.hat,
                        petFace = petLook.face,
                        petOutfit = petLook.outfit,
                        petEmotion = petLook.emotion,
                        petEyeColor = petLook.eyeColor,
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
            onTasks = {
                focusedTaskId = null
                screen = AppScreen.TASKS
            },
            onProgress = { screen = AppScreen.PROGRESS },
            onAdult = { screen = AppScreen.ADULT },
            onOpenTask = { taskId ->
                focusedTaskId = taskId
                screen = AppScreen.TASKS
            },
            onClosePeriod = requestClosePeriod,
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
                    onClosePeriod = requestClosePeriod,
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
        AppScreen.PROGRESS -> {
            val currentSnapshot = mainSnapshot
            if (currentSnapshot == null) {
                LoadingScreen()
            } else {
                ProgressScreen(
                    snapshot = currentSnapshot,
                    onBack = goBack,
                    onHint = { showHint = true },
                )
            }
        }
        AppScreen.ADULT -> {
            val currentSnapshot = mainSnapshot
            if (currentSnapshot == null) {
                LoadingScreen()
            } else {
                AdultScreen(
                    repository = repository,
                    snapshot = currentSnapshot,
                    onBack = goBack,
                    onHint = { showHint = true },
                    onDemoChanged = {
                        scope.launch { mainSnapshot = repository.loadSnapshot() }
                    },
                    onProfileDeleted = {
                        mainSnapshot = null
                        petName = ""
                        petLook = PetLook.Default
                        introPage = 0
                        screen = AppScreen.INTRO
                    },
                )
            }
        }
        AppScreen.PERIOD_RESULT -> {
            val result = periodCloseResult
            if (result == null) {
                LoadingScreen()
            } else {
                PeriodResultScreen(
                    result = result,
                    petLook = mainSnapshot?.profile?.toPetLook() ?: PetLook.Default,
                    onContinue = {
                        periodCloseResult = null
                        screen = AppScreen.MAIN
                    },
                    onHint = { showHint = true },
                )
            }
        }
    }

    if (showHint) {
        HintDialog(onClose = { showHint = false })
    }

    if (showClosePeriodConfirm) {
        AlertDialog(
            onDismissRequest = { showClosePeriodConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(text = stringResource(R.string.period_close_confirm_title)) },
            text = { Text(text = stringResource(R.string.period_close_confirm_text)) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.period_close_confirm),
                        onClick = performClosePeriod,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = stringResource(R.string.period_close_cancel),
                        onClick = { showClosePeriodConfirm = false },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
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
private fun LaunchSplashScreen() {
    Image(
        painter = painterResource(R.drawable.splash_start),
        contentDescription = stringResource(R.string.app_name),
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private fun isPlanConfirmed(snapshot: GameSnapshot?): Boolean =
    snapshot?.currentPeriod?.status == PeriodStatus.ACTIVE.name ||
        snapshot?.currentPeriod?.status == PeriodStatus.CLOSED.name