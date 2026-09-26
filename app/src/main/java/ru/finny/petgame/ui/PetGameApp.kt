package ru.finny.petgame.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.finny.petgame.R
import ru.finny.petgame.content.ContentCatalog
import ru.finny.petgame.content.ContentLoader
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.data.model.PeriodCloseResult
import ru.finny.petgame.data.model.PeriodStatus
import ru.finny.petgame.audio.BackgroundMusic
import ru.finny.petgame.data.repository.GameRepository
import ru.finny.petgame.feedback.LeafHaptics
import ru.finny.petgame.ui.components.LeafCurtain
import ru.finny.petgame.ui.components.LeafPhase
import ru.finny.petgame.ui.components.LocalAnimationsEnabled
import ru.finny.petgame.settings.UiSettings
import ru.finny.petgame.ui.components.HintDialog
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SecondaryButton
import ru.finny.petgame.ui.components.SplashLoadingBlock
import ru.finny.petgame.ui.model.PetLook
import ru.finny.petgame.ui.model.toPetLook
import ru.finny.petgame.ui.screens.AdultScreen
import ru.finny.petgame.ui.screens.IntroScreen
import ru.finny.petgame.ui.screens.MainScreen
import ru.finny.petgame.ui.screens.SettingsScreen
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
private const val SPLASH_MIN_MS = 1_000L
private const val LEAVES_FULL_BAR_PAUSE_MS = 100L
private const val LEAVES_COVER_MS = 450
private const val LEAVES_REVEAL_MS = 800

enum class AppScreen {
    LOADING,
    INTRO,
    PET,
    PET_CONFIRM,
    MAIN,
    WARDROBE,
    PLAN,
    SHOP,
    SAVINGS,
    TASKS,
    PROGRESS,
    ADULT,
    SETTINGS,
    PERIOD_RESULT,
}

@Composable
fun PetGameApp(
    repository: GameRepository,
    music: BackgroundMusic,
    uiSettings: UiSettings,
) {
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
    var catalog by remember { mutableStateOf<ContentCatalog?>(null) }
    val context = LocalContext.current

    ReportDrawnWhen { screen != AppScreen.LOADING }

    val animationsOn = LocalAnimationsEnabled.current
    var leafPhase by remember { mutableStateOf<LeafPhase?>(null) }
    val leafProgress = remember { Animatable(0f) }
    val splashProgress = remember { Animatable(0f) }
    val view = LocalView.current
    val leafHaptics = remember(view) { LeafHaptics(view) }

    LaunchedEffect(Unit) {
        val bar = launch {
            splashProgress.animateTo(1f, tween(SPLASH_MIN_MS.toInt(), easing = LinearEasing))
        }
        catalog = withContext(Dispatchers.IO) { ContentLoader(context.assets).loadCatalog() }
        val snapshot = repository.loadSnapshot()
        mainSnapshot = snapshot
        bar.join()
        val firstScreen = if (snapshot != null) AppScreen.MAIN else AppScreen.INTRO
        if (!animationsOn) {
            screen = firstScreen
            return@LaunchedEffect
        }
        delay(LEAVES_FULL_BAR_PAUSE_MS)
        leafPhase = LeafPhase.COVER
        leafProgress.snapTo(0f)
        launch { leafHaptics.cover(LEAVES_COVER_MS) }
        leafProgress.animateTo(1f, tween(LEAVES_COVER_MS, easing = LinearEasing))
        leafHaptics.covered()
        screen = firstScreen
        delay(90)
        leafPhase = LeafPhase.REVEAL
        leafProgress.snapTo(0f)
        launch { leafHaptics.reveal(LEAVES_REVEAL_MS) }
        leafProgress.animateTo(1f, tween(LEAVES_REVEAL_MS, easing = LinearEasing))
        leafPhase = null
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
            AppScreen.WARDROBE -> screen = AppScreen.MAIN
            AppScreen.PLAN -> screen = AppScreen.MAIN
            AppScreen.SHOP -> screen = AppScreen.MAIN
            AppScreen.SAVINGS -> screen = AppScreen.MAIN
            AppScreen.TASKS -> screen = AppScreen.MAIN
            AppScreen.PROGRESS -> screen = AppScreen.MAIN
            AppScreen.ADULT -> screen = AppScreen.MAIN
            AppScreen.SETTINGS -> screen = AppScreen.MAIN
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
            val periodIndex = mainSnapshot?.currentPeriod?.periodIndex ?: 0
            val needs = catalog?.needsFor(periodIndex).orEmpty()
            when (val result = repository.closePeriod(needs)) {
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

    Box(modifier = Modifier.fillMaxSize()) {
        when (screen) {
            AppScreen.LOADING -> LaunchSplashScreen(progress = splashProgress.value)
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
                catalog = catalog,
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
                onSettings = { screen = AppScreen.SETTINGS },
                seenSceneGoals = uiSettings.seenSceneGoals,
                onSceneGoalsShown = uiSettings::markSceneGoalsSeen,
                onWardrobe = {
                    val profile = mainSnapshot?.profile
                    if (profile != null) {
                        petLook = profile.toPetLook()
                        petName = profile.petName
                        screen = AppScreen.WARDROBE
                    }
                },
            )
            AppScreen.WARDROBE -> PetScreen(
                look = petLook,
                petName = petName,
                onLookChange = { petLook = it },
                onPetNameChange = { petName = it },
                onBack = goBack,
                onHint = { showHint = true },
                editMode = true,
                onNext = {
                    scope.launch {
                        val saved = repository.updatePetAppearance(
                            petName = petName,
                            petHat = petLook.hat,
                            petFace = petLook.face,
                            petOutfit = petLook.outfit,
                            petEmotion = petLook.emotion,
                            petEyeColor = petLook.eyeColor,
                        )
                        if (saved) {
                            mainSnapshot = repository.loadSnapshot()
                            screen = AppScreen.MAIN
                        }
                    }
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
                        catalog = catalog,
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
                        onOpenTasks = {
                            focusedTaskId = null
                            screen = AppScreen.TASKS
                        },
                        onOpenSavings = { screen = AppScreen.SAVINGS },
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
                        onOpenSavings = { screen = AppScreen.SAVINGS },
                    )
                }
            }
            AppScreen.PROGRESS -> {
                val currentSnapshot = mainSnapshot
                if (currentSnapshot == null) {
                    LoadingScreen()
                } else {
                    ProgressScreen(
                        repository = repository,
                        snapshot = currentSnapshot,
                        onBack = goBack,
                        onHint = { showHint = true },
                    )
                }
            }
            AppScreen.SETTINGS -> SettingsScreen(
                musicOn = music.enabled,
                volume = music.volume,
                onMusicOnChange = music::updateEnabled,
                onVolumeChange = music::updateVolume,
                onVolumeChangeFinished = music::saveVolume,
                animationsOn = uiSettings.animationsEnabled,
                systemAnimationsOff = uiSettings.systemAnimationsOff,
                onAnimationsOnChange = uiSettings::updateAnimationsEnabled,
                onBack = goBack,
                onHint = { showHint = true },
            )
            AppScreen.ADULT -> {
                val currentSnapshot = mainSnapshot
                if (currentSnapshot == null) {
                    LoadingScreen()
                } else {
                    AdultScreen(
                        repository = repository,
                        snapshot = currentSnapshot,
                        accessoryPurse = catalog?.accessoryPurse() ?: 0L,
                        onBack = goBack,
                        onHint = { showHint = true },
                        onDemoChanged = {
                            scope.launch { mainSnapshot = repository.loadSnapshot() }
                        },
                        onProfileDeleted = {
                            uiSettings.clearSceneGoalsSeen()
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
        leafPhase?.let { phase ->
            LeafCurtain(phase = phase, progress = leafProgress.value)
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
private fun LaunchSplashScreen(progress: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.splash_start),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        SplashLoadingBlock(
            progress = progress,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 108.dp),
        )
    }
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