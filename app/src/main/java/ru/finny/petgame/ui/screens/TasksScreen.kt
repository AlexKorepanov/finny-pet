package ru.finny.petgame.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.finny.petgame.R
import ru.finny.petgame.ui.components.sectionBackground
import ru.finny.petgame.ui.components.SectionAccent
import ru.finny.petgame.audio.SoundEffect
import ru.finny.petgame.content.ContentCatalog
import ru.finny.petgame.content.ContentLoader
import ru.finny.petgame.content.openTaskDay
import ru.finny.petgame.content.model.TaskContent
import ru.finny.petgame.content.model.TaskTheme
import ru.finny.petgame.content.model.TaskType
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.data.model.TaskCompletionResult
import ru.finny.petgame.data.repository.GameRepository
import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.economy.model.DistributionCheck
import ru.finny.petgame.ui.components.AmountStepper
import ru.finny.petgame.ui.components.AppCard
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.BadgeKind
import ru.finny.petgame.ui.components.ChunkySurface
import ru.finny.petgame.ui.components.LocalAnimationsEnabled
import ru.finny.petgame.ui.components.PetSprite
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SecondaryButton
import ru.finny.petgame.ui.components.StatusBadge
import ru.finny.petgame.ui.components.coinsAmount
import ru.finny.petgame.ui.model.PetLook
import ru.finny.petgame.ui.model.toPetLook
import ru.finny.petgame.ui.theme.FinnyColors
import kotlin.math.min

/** Урок — задания одного учебного дня. Код урока: неделя × 10 + день, чтобы пережить поворот экрана. */
private fun lessonCode(week: Int, day: Int) = week * 10 + day

@Composable
fun TasksScreen(
    repository: GameRepository,
    snapshot: GameSnapshot,
    focusedTaskId: String?,
    onSound: (SoundEffect) -> Unit,
    onBack: () -> Unit,
    onHint: () -> Unit,
    onTasksChanged: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenSavings: () -> Unit,
) {
    val context = LocalContext.current
    var catalog by remember { mutableStateOf<ContentCatalog?>(null) }
    var openLesson by rememberSaveable { mutableStateOf<Int?>(null) }
    var startTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var focusConsumed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        catalog = withContext(Dispatchers.IO) {
            ContentLoader(context.assets).loadCatalog()
        }
    }
    val loadedCatalog = catalog
    LaunchedEffect(loadedCatalog) {
        if (loadedCatalog != null && !focusConsumed) {
            focusConsumed = true
            loadedCatalog.tasks.firstOrNull { it.id == focusedTaskId }?.let { task ->
                openLesson = lessonCode(task.week, task.day)
                startTaskId = task.id
            }
        }
    }
    BackHandler(enabled = openLesson != null) {
        openLesson = null
        startTaskId = null
    }

    val completedIds = snapshot.completedTasks.filter { it.isCorrect == true }.map { it.taskId }.toSet()
    val periodIndex = snapshot.currentPeriod?.periodIndex ?: 0
    val openDay = snapshot.currentPeriod?.let { openTaskDay(it.createdAt) } ?: 1
    val allOpen = snapshot.profile.allTasksOpen
    val lesson = openLesson

    if (loadedCatalog != null && lesson != null) {
        val week = lesson / 10
        val day = lesson % 10
        key(lesson) {
            LessonScreen(
                repository = repository,
                dayTasks = loadedCatalog.tasks.filter { it.week == week && it.day == day },
                completedAtStart = completedIds,
                startTaskId = startTaskId,
                weekLesson = loadedCatalog.weeks.firstOrNull { it.index == week }?.lesson,
                petLook = snapshot.profile.toPetLook(),
                petStage = snapshot.profile.petStage,
                onSound = onSound,
                onTasksChanged = onTasksChanged,
                onClose = {
                    openLesson = null
                    startTaskId = null
                },
                onTryInGame = { theme ->
                    when (theme) {
                        TaskTheme.BUDGET -> onOpenPlan()
                        TaskTheme.SAVINGS -> onOpenSavings()
                        TaskTheme.PAYMENTS -> onOpenShop()
                    }
                },
            )
        }
        return
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.section_tasks),
                showBack = true,
                onBack = onBack,
                onHint = onHint,
                accent = SectionAccent.TASKS,
            )
        },
        containerColor = sectionBackground(SectionAccent.TASKS),
        contentColor = FinnyColors.TextPrimary,
    ) { padding ->
        if (loadedCatalog == null) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LessonPath(
                catalog = loadedCatalog,
                completedIds = completedIds,
                periodIndex = periodIndex,
                openDay = openDay,
                allOpen = allOpen,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                onOpenLesson = { week, day ->
                    startTaskId = null
                    openLesson = lessonCode(week, day)
                },
            )
        }
    }
}

// ---------- Тропинка уроков ----------

private data class DayNode(
    val week: Int,
    val day: Int,
    val total: Int,
    val done: Int,
    val open: Boolean,
) {
    val finished: Boolean get() = total > 0 && done >= total
}

/** Смещения кружков по горизонтали — тропинка «змейкой», как в обучающих играх. */
private val PATH_OFFSETS = listOf(0.dp, 44.dp, 66.dp, 44.dp, 0.dp, (-44).dp, (-66).dp)

@Composable
private fun LessonPath(
    catalog: ContentCatalog,
    completedIds: Set<String>,
    periodIndex: Int,
    openDay: Int,
    allOpen: Boolean,
    modifier: Modifier = Modifier,
    onOpenLesson: (week: Int, day: Int) -> Unit,
) {
    val weeks = catalog.tasks.groupBy { it.week }.toSortedMap().map { (week, tasks) ->
        week to tasks.groupBy { it.day }.toSortedMap().map { (day, dayTasks) ->
            DayNode(
                week = week,
                day = day,
                total = dayTasks.size,
                done = dayTasks.count { it.id in completedIds },
                open = dayTasks.any { catalog.isTaskOpen(it, periodIndex, openDay, allOpen) },
            )
        }
    }
    val current = weeks.flatMap { it.second }.firstOrNull { it.open && !it.finished }
    val listState = rememberLazyListState()
    val headerItems = if (allOpen) 1 else 0
    LaunchedEffect(current?.week) {
        val index = weeks.indexOfFirst { it.first == current?.week }
        if (index > 0) listState.scrollToItem(index + headerItems)
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (allOpen) {
            item {
                StatusBadge(
                    text = stringResource(R.string.tasks_demo_badge),
                    icon = Icons.Filled.Done,
                    kind = BadgeKind.NEUTRAL,
                )
            }
        }
        items(weeks, key = { it.first }) { (week, days) ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WeekBanner(
                    week = week,
                    theme = catalog.weeks.firstOrNull { it.index == week }?.theme,
                    locked = days.none { it.open },
                    finished = days.all { it.finished },
                )
                val side = if (week % 2 == 1) 1 else -1
                days.forEachIndexed { index, node ->
                    PathNode(
                        node = node,
                        current = node == current,
                        offset = PATH_OFFSETS[index % PATH_OFFSETS.size] * side,
                        onClick = { onOpenLesson(node.week, node.day) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekBanner(week: Int, theme: String?, locked: Boolean, finished: Boolean) {
    val shape = RoundedCornerShape(20.dp)
    val face = if (locked) FinnyColors.LockedFace else FinnyColors.Tasks
    val edge = if (locked) FinnyColors.LockedEdge else FinnyColors.TasksEdge
    val content = if (locked) FinnyColors.TextSecondary else FinnyColors.OnPrimary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(edge)
            .padding(bottom = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(face)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tasks_week_title_short, week),
                    style = MaterialTheme.typography.labelLarge,
                    color = content.copy(alpha = 0.85f),
                )
                if (theme != null) {
                    Text(
                        text = theme,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = content,
                    )
                }
                if (locked) {
                    Text(
                        text = stringResource(R.string.tasks_locked_week, week),
                        style = MaterialTheme.typography.bodyMedium,
                        color = content,
                    )
                }
            }
            when {
                locked -> Icon(Icons.Filled.Lock, contentDescription = null, tint = content, modifier = Modifier.size(28.dp))
                finished -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
                    Text(
                        text = stringResource(R.string.path_week_done),
                        style = MaterialTheme.typography.labelLarge,
                        color = content,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PathNode(node: DayNode, current: Boolean, offset: Dp, onClick: () -> Unit) {
    val dayName = weekdayShort(node.day)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.offset(x = offset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (current) StartBubble()
            val description = if (node.open) {
                stringResource(R.string.path_node_open, dayName, node.done, node.total)
            } else {
                stringResource(R.string.path_node_locked, dayName)
            }
            PathButton(
                face = when {
                    !node.open -> FinnyColors.LockedFace
                    node.finished -> FinnyColors.ProgressYellow
                    else -> FinnyColors.Tasks
                },
                edge = when {
                    !node.open -> FinnyColors.LockedEdge
                    node.finished -> FinnyColors.ProgressYellowEdge
                    else -> FinnyColors.TasksEdge
                },
                icon = when {
                    !node.open -> Icons.Filled.Lock
                    node.finished -> Icons.Filled.Check
                    else -> Icons.Filled.Star
                },
                iconTint = when {
                    !node.open -> FinnyColors.TextSecondary
                    node.finished -> FinnyColors.OnProgressText
                    else -> FinnyColors.OnPrimary
                },
                enabled = node.open,
                highlighted = current,
                contentDescription = description,
                onClick = onClick,
            )
            Text(
                text = if (node.open) {
                    stringResource(R.string.path_day_progress, dayName, node.done, node.total)
                } else {
                    dayName
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (node.open) FinnyColors.TextPrimary else FinnyColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun StartBubble() {
    Box(
        modifier = Modifier
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, FinnyColors.Tasks, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = stringResource(R.string.path_start),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = FinnyColors.Tasks,
        )
    }
}

@Composable
private fun PathButton(
    face: Color,
    edge: Color,
    icon: ImageVector,
    iconTint: Color,
    enabled: Boolean,
    highlighted: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val size = 76.dp
    Box(
        modifier = Modifier
            .then(
                if (highlighted) {
                    Modifier
                        .border(4.dp, face.copy(alpha = 0.35f), CircleShape)
                        .padding(6.dp)
                } else {
                    Modifier.padding(10.dp)
                },
            )
            .size(width = size, height = size + 6.dp)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .offset(y = 6.dp)
                .size(size)
                .clip(CircleShape)
                .background(edge),
        )
        Box(
            modifier = Modifier
                .offset(y = if (pressed && enabled) 6.dp else 0.dp)
                .size(size)
                .clip(CircleShape)
                .background(face),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(36.dp))
        }
    }
}

// ---------- Урок ----------

private data class StepFeedback(
    val correct: Boolean,
    val coins: Long,
    val firstTry: Boolean,
    val practice: Boolean,
    val result: String?,
    val explanation: String,
    val problems: List<String> = emptyList(),
)

@Composable
private fun LessonScreen(
    repository: GameRepository,
    dayTasks: List<TaskContent>,
    completedAtStart: Set<String>,
    startTaskId: String?,
    weekLesson: String?,
    petLook: PetLook,
    petStage: Int,
    onSound: (SoundEffect) -> Unit,
    onTasksChanged: () -> Unit,
    onClose: () -> Unit,
    onTryInGame: (TaskTheme) -> Unit,
) {
    val queue = remember {
        val practice = dayTasks.all { it.id in completedAtStart }
        val base = if (practice) dayTasks else dayTasks.filter { it.id !in completedAtStart }
        val first = base.firstOrNull { it.id == startTaskId }
        mutableStateListOf<TaskContent>().apply {
            first?.let { add(it) }
            addAll(base.filter { it.id != first?.id })
        }
    }
    val total = remember { dayTasks.size.coerceAtLeast(1) }
    val solvedBefore = remember { dayTasks.size - queue.size }
    var solved by remember { mutableIntStateOf(0) }
    var earned by remember { mutableLongStateOf(0L) }
    var step by remember { mutableIntStateOf(0) }
    var showTip by remember { mutableStateOf(weekLesson != null && queue.isNotEmpty()) }
    val mainTheme = remember {
        dayTasks.groupingBy { it.theme }.eachCount().maxByOrNull { it.value }?.key ?: TaskTheme.BUDGET
    }

    Scaffold(
        containerColor = sectionBackground(SectionAccent.TASKS),
        contentColor = FinnyColors.TextPrimary,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            LessonTopBar(solved = solvedBefore + solved, total = total, earned = earned, onClose = onClose)
            val task = queue.firstOrNull()
            when {
                task == null -> LessonComplete(
                    petLook = petLook,
                    petStage = petStage,
                    earned = earned,
                    solved = solved,
                    weekLesson = weekLesson,
                    theme = mainTheme,
                    onTryInGame = onTryInGame,
                    onClose = onClose,
                )
                showTip -> LessonTip(
                    text = weekLesson.orEmpty(),
                    petLook = petLook,
                    petStage = petStage,
                    onStart = { showTip = false },
                )
                else -> key(step) {
                    TaskStep(
                        repository = repository,
                        task = task,
                        petLook = petLook,
                        petStage = petStage,
                        onSound = onSound,
                        onTasksChanged = onTasksChanged,
                        onContinue = { correct, coins ->
                            val done = queue.removeAt(0)
                            if (correct) {
                                solved++
                                earned += coins
                            } else {
                                queue.add(done)
                            }
                            step++
                            if (queue.isEmpty()) onSound(SoundEffect.LESSON_DONE)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonTopBar(solved: Int, total: Int, earned: Long, onClose: () -> Unit) {
    val animations = LocalAnimationsEnabled.current
    val target = solved.toFloat() / total
    val progress by animateFloatAsState(targetValue = target, label = "lessonProgress")
    val shown = if (animations) progress else target
    val progressLabel = stringResource(R.string.lesson_progress, solved, total)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.lesson_close),
                tint = FinnyColors.TextSecondary,
                modifier = Modifier.size(30.dp),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.outlineVariant)
                .semantics { contentDescription = progressLabel },
        ) {
            if (shown > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(shown.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(FinnyColors.Success),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.35f)),
                    )
                }
            }
        }
        Text(
            text = "🪙 $earned",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = FinnyColors.TextPrimary,
            modifier = Modifier.padding(start = 12.dp, end = 8.dp),
        )
    }
}

@Composable
private fun LessonTip(text: String, petLook: PetLook, petStage: Int, onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = stringResource(R.string.lesson_tip_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = FinnyColors.TextPrimary,
            )
            FinnySays(text = text, look = petLook.withEmotion(1), stage = petStage, spriteSize = 140.dp)
        }
        PrimaryButton(
            text = stringResource(R.string.lesson_start),
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            containerColor = FinnyColors.Success,
            edgeColor = FinnyColors.SuccessEdge,
        )
    }
}

@Composable
private fun TaskStep(
    repository: GameRepository,
    task: TaskContent,
    petLook: PetLook,
    petStage: Int,
    onSound: (SoundEffect) -> Unit,
    onTasksChanged: () -> Unit,
    onContinue: (correct: Boolean, coins: Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val options = remember { task.options.shuffled() }
    var chosenId by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<StepFeedback?>(null) }
    var checking by remember { mutableStateOf(false) }
    var requiredAmount by remember { mutableLongStateOf(0L) }
    var optionalAmount by remember { mutableLongStateOf(0L) }
    var savingsAmount by remember { mutableLongStateOf(0L) }
    val amounts = mapOf(
        BudgetDirection.REQUIRED to requiredAmount,
        BudgetDirection.OPTIONAL to optionalAmount,
        BudgetDirection.SAVINGS to savingsAmount,
    )
    val canCheck = when (task.type) {
        TaskType.CHOICE -> chosenId != null
        TaskType.DISTRIBUTE -> amounts.values.sum() > 0L
    }

    fun check() {
        if (checking) return
        checking = true
        scope.launch {
            val chosen = task.options.firstOrNull { it.id == chosenId }
            val problems: List<String>
            val correct: Boolean
            when (task.type) {
                TaskType.CHOICE -> {
                    correct = chosen?.isGood == true
                    problems = emptyList()
                }
                TaskType.DISTRIBUTE -> {
                    val result = repository.checkTaskDistribution(amounts, task.sum, task.minimums)
                    correct = result is DistributionCheck.Valid
                    problems = (result as? DistributionCheck.Invalid)?.problems.orEmpty()
                }
            }
            val result = repository.completeTask(
                taskId = task.id,
                taskTitle = task.title,
                theme = task.theme.name,
                isCorrect = correct,
                reward = task.reward,
            )
            val success = result as? TaskCompletionResult.Success
            onSound(if (correct) SoundEffect.CORRECT else SoundEffect.WRONG)
            feedback = StepFeedback(
                correct = correct,
                coins = if (correct) success?.reward ?: 0L else 0L,
                firstTry = success?.firstTry ?: true,
                practice = result is TaskCompletionResult.AlreadyCompleted,
                result = chosen?.result,
                explanation = when {
                    result is TaskCompletionResult.Invalid -> result.explanation
                    correct -> task.correctExplanation
                    else -> task.wrongExplanation
                },
                problems = problems,
            )
            checking = false
            onTasksChanged()
        }
    }

    val current = feedback
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ThemeChip(task.theme)
            Text(
                text = task.question,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = FinnyColors.TextPrimary,
            )
            FinnySays(
                text = task.story,
                look = when {
                    current?.correct == true -> petLook.withEmotion(3)
                    else -> petLook
                },
                stage = petStage,
                spriteSize = 112.dp,
            )
            when (task.type) {
                TaskType.CHOICE -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    options.forEach { option ->
                        val state = when {
                            current == null && option.id == chosenId -> AnswerState.SELECTED
                            current == null -> AnswerState.IDLE
                            option.id == chosenId && current.correct -> AnswerState.CORRECT
                            option.id == chosenId -> AnswerState.WRONG
                            else -> AnswerState.DIMMED
                        }
                        AnswerButton(
                            text = option.text,
                            state = state,
                            onClick = { chosenId = option.id },
                        )
                    }
                }
                TaskType.DISTRIBUTE -> AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.tasks_distribute_sum, coinsAmount(task.sum)),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        BudgetDirection.entries.forEach { direction ->
                            val value = amounts[direction] ?: 0L
                            fun set(newValue: Long) {
                                when (direction) {
                                    BudgetDirection.REQUIRED -> requiredAmount = newValue
                                    BudgetDirection.OPTIONAL -> optionalAmount = newValue
                                    BudgetDirection.SAVINGS -> savingsAmount = newValue
                                }
                            }
                            AmountStepper(
                                label = directionTitle(direction),
                                caption = directionCaption(direction),
                                amount = value,
                                canIncrease = current == null && amounts.values.sum() < task.sum,
                                canDecrease = current == null && value > 0L,
                                onIncrease = { set(value + 1L) },
                                onDecrease = { set(value - 1L) },
                                onAmountSet = { if (current == null) set(it) },
                                maxAmount = task.sum - (amounts.values.sum() - value),
                            )
                        }
                        Text(
                            text = stringResource(
                                R.string.tasks_distribute_left,
                                coinsAmount((task.sum - amounts.values.sum()).coerceAtLeast(0L)),
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }

        val animations = LocalAnimationsEnabled.current
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                if (animations) {
                    (slideInVertically { it / 2 } + fadeIn()) togetherWith ExitTransition.None
                } else {
                    EnterTransition.None togetherWith ExitTransition.None
                }
            },
            label = "lessonFeedback",
        ) { shown ->
            if (shown == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.lesson_check),
                        onClick = ::check,
                        enabled = canCheck && !checking,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = FinnyColors.Success,
                        edgeColor = FinnyColors.SuccessEdge,
                    )
                }
            } else {
                FeedbackPanel(feedback = shown, onContinue = { onContinue(shown.correct, shown.coins) })
            }
        }
    }
}

private enum class AnswerState { IDLE, SELECTED, CORRECT, WRONG, DIMMED }

@Composable
private fun AnswerButton(text: String, state: AnswerState, onClick: () -> Unit) {
    val (container, edge, border) = when (state) {
        AnswerState.IDLE, AnswerState.DIMMED -> Triple(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant)
        AnswerState.SELECTED -> Triple(FinnyColors.SoftBlue, FinnyColors.PrimaryEdge, FinnyColors.Primary)
        AnswerState.CORRECT -> Triple(FinnyColors.SoftGreen, FinnyColors.SuccessEdge, FinnyColors.Success)
        AnswerState.WRONG -> Triple(FinnyColors.WrongContainer, FinnyColors.WrongEdge, FinnyColors.Wrong)
    }
    val textColor = when (state) {
        AnswerState.SELECTED -> FinnyColors.PrimaryEdge
        AnswerState.CORRECT -> FinnyColors.SuccessEdge
        AnswerState.WRONG -> FinnyColors.WrongEdge
        AnswerState.DIMMED -> FinnyColors.TextSecondary
        AnswerState.IDLE -> FinnyColors.TextPrimary
    }
    val interactive = state == AnswerState.IDLE || state == AnswerState.SELECTED
    ChunkySurface(
        onClick = if (interactive) onClick else null,
        modifier = Modifier.fillMaxWidth(),
        selected = state != AnswerState.IDLE && state != AnswerState.DIMMED,
        containerColor = container,
        edgeColor = edge,
        borderColor = border,
        borderWidth = if (interactive && state == AnswerState.IDLE) 2.dp else 3.dp,
        minHeight = 60.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            when (state) {
                AnswerState.CORRECT -> Icon(Icons.Filled.Check, null, tint = textColor, modifier = Modifier.size(22.dp))
                AnswerState.WRONG -> Icon(Icons.Filled.Close, null, tint = textColor, modifier = Modifier.size(22.dp))
                else -> {}
            }
            if (state == AnswerState.CORRECT || state == AnswerState.WRONG) Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FeedbackPanel(feedback: StepFeedback, onContinue: () -> Unit) {
    val correctTitles = stringArrayResource(R.array.lesson_correct_titles)
    val title = remember(feedback) {
        if (feedback.correct) correctTitles.random() else null
    } ?: stringResource(R.string.lesson_wrong_title)
    val accent = if (feedback.correct) FinnyColors.Success else FinnyColors.Wrong
    val accentEdge = if (feedback.correct) FinnyColors.SuccessEdge else FinnyColors.WrongEdge
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (feedback.correct) FinnyColors.SoftGreen else FinnyColors.WrongContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (feedback.correct) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    tint = FinnyColors.OnPrimary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentEdge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            )
            if (feedback.coins > 0L) {
                Text(
                    text = "🪙 " + stringResource(R.string.lesson_coins, feedback.coins),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentEdge,
                )
            }
        }
        feedback.result?.let {
            Text(text = it, style = MaterialTheme.typography.bodyLarge, color = FinnyColors.TextPrimary)
        }
        feedback.problems.forEach {
            Text(text = it, style = MaterialTheme.typography.bodyLarge, color = FinnyColors.TextPrimary)
        }
        Text(
            text = stringResource(
                if (feedback.correct) R.string.lesson_remember else R.string.lesson_hint,
                feedback.explanation,
            ),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = accentEdge,
        )
        when {
            !feedback.correct -> Text(
                text = stringResource(R.string.lesson_comes_back),
                style = MaterialTheme.typography.bodyMedium,
                color = FinnyColors.TextSecondary,
            )
            feedback.practice -> Text(
                text = stringResource(R.string.lesson_practice),
                style = MaterialTheme.typography.bodyMedium,
                color = FinnyColors.TextSecondary,
            )
            !feedback.firstTry && feedback.coins > 0L -> Text(
                text = stringResource(R.string.tasks_retry_reward_line, feedback.coins),
                style = MaterialTheme.typography.bodyMedium,
                color = FinnyColors.TextSecondary,
            )
        }
        PrimaryButton(
            text = stringResource(if (feedback.correct) R.string.lesson_next else R.string.lesson_got_it),
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            containerColor = accent,
            edgeColor = accentEdge,
        )
    }
}

@Composable
private fun LessonComplete(
    petLook: PetLook,
    petStage: Int,
    earned: Long,
    solved: Int,
    weekLesson: String?,
    theme: TaskTheme,
    onTryInGame: (TaskTheme) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            PetSprite(look = petLook.withEmotion(3), stage = petStage, modifier = Modifier.size(170.dp))
            Text(
                text = stringResource(R.string.lesson_done_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = FinnyColors.ProgressYellowEdge,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (earned > 0L) {
                    StatTile(
                        value = "🪙 " + stringResource(R.string.lesson_coins, earned),
                        label = stringResource(R.string.lesson_done_coins),
                        color = FinnyColors.ProgressYellowEdge,
                        modifier = Modifier.weight(1f),
                    )
                }
                StatTile(
                    value = "✅ $solved",
                    label = stringResource(R.string.lesson_done_tasks),
                    color = FinnyColors.Success,
                    modifier = Modifier.weight(1f),
                )
            }
            if (weekLesson != null) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.lesson_done_learned),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(text = weekLesson, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                text = tryInGameLabel(theme),
                onClick = { onTryInGame(theme) },
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = stringResource(R.string.lesson_back_to_path),
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .border(2.dp, color, shape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = FinnyColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ThemeChip(theme: TaskTheme) {
    val (emoji, label) = when (theme) {
        TaskTheme.BUDGET -> "📋" to stringResource(R.string.tasks_theme_budget)
        TaskTheme.SAVINGS -> "🐷" to stringResource(R.string.tasks_theme_savings)
        TaskTheme.PAYMENTS -> "🛒" to stringResource(R.string.tasks_theme_payments)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(FinnyColors.SoftPurple)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, fontSize = 16.sp)
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = FinnyColors.TasksText,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** Финни и облачко с репликой, как персонаж-рассказчик в обучающих играх. */
@Composable
private fun FinnySays(text: String, look: PetLook, stage: Int, spriteSize: Dp) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PetSprite(look = look, stage = stage, modifier = Modifier.size(spriteSize))
        val shape = remember { BubbleShape(tail = 12.dp, corner = 16.dp) }
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .padding(start = 12.dp + 12.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge, color = FinnyColors.TextPrimary)
        }
    }
}

/** Облачко с хвостиком слева, направленным на Финни. */
private class BubbleShape(private val tail: Dp, private val corner: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val t = with(density) { tail.toPx() }
        val r = with(density) { corner.toPx() }
        val body = Path().apply {
            addRoundRect(RoundRect(t, 0f, size.width, size.height, CornerRadius(r)))
        }
        val tipY = min(size.height / 2f, with(density) { 32.dp.toPx() })
        val tip = Path().apply {
            moveTo(t + r / 2f, tipY - t)
            lineTo(0f, tipY)
            lineTo(t + r / 2f, tipY + t)
            close()
        }
        return Outline.Generic(Path.combine(PathOperation.Union, body, tip))
    }
}

@Composable
private fun tryInGameLabel(theme: TaskTheme): String = when (theme) {
    TaskTheme.BUDGET -> stringResource(R.string.tasks_try_plan)
    TaskTheme.SAVINGS -> stringResource(R.string.tasks_try_savings)
    TaskTheme.PAYMENTS -> stringResource(R.string.tasks_try_shop)
}

@Composable
private fun weekdayShort(day: Int): String = stringResource(
    when (day) {
        2 -> R.string.tasks_weekday_2
        3 -> R.string.tasks_weekday_3
        4 -> R.string.tasks_weekday_4
        5 -> R.string.tasks_weekday_5
        6 -> R.string.tasks_weekday_6
        7 -> R.string.tasks_weekday_7
        else -> R.string.tasks_weekday_1
    },
)

@Composable
private fun directionTitle(direction: BudgetDirection): String = when (direction) {
    BudgetDirection.REQUIRED -> stringResource(R.string.plan_direction_required)
    BudgetDirection.OPTIONAL -> stringResource(R.string.plan_direction_optional)
    BudgetDirection.SAVINGS -> stringResource(R.string.plan_direction_savings)
}

@Composable
private fun directionCaption(direction: BudgetDirection): String = when (direction) {
    BudgetDirection.REQUIRED -> stringResource(R.string.plan_hint_required)
    BudgetDirection.OPTIONAL -> stringResource(R.string.plan_hint_optional)
    BudgetDirection.SAVINGS -> stringResource(R.string.plan_hint_savings)
}
