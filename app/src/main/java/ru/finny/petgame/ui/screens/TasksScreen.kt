package ru.finny.petgame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.finny.petgame.R
import ru.finny.petgame.content.ContentCatalog
import ru.finny.petgame.content.ContentLoader
import ru.finny.petgame.content.model.TaskContent
import ru.finny.petgame.content.model.TaskOption
import ru.finny.petgame.content.model.TaskTheme
import ru.finny.petgame.content.model.TaskType
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.data.model.TaskCompletionResult
import ru.finny.petgame.data.repository.GameRepository
import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.economy.model.DistributionCheck
import ru.finny.petgame.ui.components.AppCard
import ru.finny.petgame.ui.components.ChunkySurface
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.AmountStepper
import ru.finny.petgame.ui.components.BadgeKind
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SectionAccent
import ru.finny.petgame.ui.components.SectionHeader
import ru.finny.petgame.ui.model.toPetLook
import ru.finny.petgame.ui.components.SectionTitle
import ru.finny.petgame.ui.components.SecondaryButton
import ru.finny.petgame.ui.components.StatusBadge
import ru.finny.petgame.ui.components.coinsAmount
import ru.finny.petgame.ui.theme.FinnyColors

@Composable
fun TasksScreen(
    repository: GameRepository,
    snapshot: GameSnapshot,
    focusedTaskId: String?,
    onBack: () -> Unit,
    onHint: () -> Unit,
    onTasksChanged: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenSavings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var catalog by remember { mutableStateOf<ContentCatalog?>(null) }
    var currentTaskId by rememberSaveable { mutableStateOf(focusedTaskId) }
    var attempt by remember(currentTaskId) { mutableIntStateOf(0) }
    var chosenOptionId by remember(currentTaskId) { mutableStateOf<String?>(null) }
    var feedback by remember(currentTaskId) { mutableStateOf<TaskCompletionResult?>(null) }
    var distributionCheck by remember(currentTaskId) { mutableStateOf<DistributionCheck?>(null) }
    var requiredAmount by remember(currentTaskId) { mutableLongStateOf(0L) }
    var optionalAmount by remember(currentTaskId) { mutableLongStateOf(0L) }
    var savingsAmount by remember(currentTaskId) { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        catalog = withContext(Dispatchers.IO) {
            ContentLoader(context.assets).loadCatalog()
        }
    }

    val completedIds = snapshot.completedTasks.filter { it.isCorrect == true }.map { it.taskId }.toSet()
    val periodIndex = snapshot.currentPeriod?.periodIndex ?: 0
    val allOpen = snapshot.profile.allTasksOpen
    val loadedCatalog = catalog
    val taskList = loadedCatalog?.tasks
    val currentTask = taskList?.firstOrNull { it.id == currentTaskId }
    val shuffledOptions = remember(currentTask, attempt) { currentTask?.options?.shuffled().orEmpty() }
    val nextIncompleteTask = taskList
        ?.filter { it.id != currentTaskId && it.id !in completedIds }
        ?.firstOrNull { loadedCatalog.isTaskOpen(it, periodIndex, allOpen) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_name),
                showBack = true,
                onBack = onBack,
                onHint = onHint,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                accent = SectionAccent.TASKS,
                title = stringResource(R.string.section_tasks),
                hint = stringResource(R.string.tasks_header_hint),
                petLook = snapshot.profile.toPetLook(),
                petStage = snapshot.profile.petStage,
            )
            if (loadedCatalog == null || taskList == null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (currentTask == null) {
                if (allOpen) {
                    StatusBadge(
                        text = stringResource(R.string.tasks_demo_badge),
                        icon = Icons.Filled.Done,
                        kind = BadgeKind.NEUTRAL,
                    )
                }
                taskList.groupBy { it.week }.toSortedMap().forEach { (week, weekTasks) ->
                    val weekTheme = loadedCatalog.weeks.firstOrNull { it.index == week }?.theme
                    SectionTitle(
                        text = if (weekTheme != null) {
                            stringResource(R.string.tasks_week_title, week, weekTheme)
                        } else {
                            stringResource(R.string.tasks_week_title_short, week)
                        },
                        accent = SectionAccent.TASKS,
                    )
                    weekTasks.forEach { task ->
                        TaskCard(
                            task = task,
                            completed = task.id in completedIds,
                            available = loadedCatalog.isTaskOpen(task, periodIndex, allOpen),
                            onClick = { currentTaskId = task.id },
                        )
                    }
                }
            } else {
                TaskPlayView(
                    task = currentTask,
                    options = shuffledOptions,
                    chosenOptionId = chosenOptionId,
                    feedback = feedback,
                    distributionCheck = distributionCheck,
                    amounts = mapOf(
                        BudgetDirection.REQUIRED to requiredAmount,
                        BudgetDirection.OPTIONAL to optionalAmount,
                        BudgetDirection.SAVINGS to savingsAmount,
                    ),
                    nextTaskId = nextIncompleteTask?.id,
                    onDistributeChange = { direction, value ->
                        when (direction) {
                            BudgetDirection.REQUIRED -> requiredAmount = value
                            BudgetDirection.OPTIONAL -> optionalAmount = value
                            BudgetDirection.SAVINGS -> savingsAmount = value
                        }
                    },
                    onChooseOption = { option ->
                        chosenOptionId = option.id
                        distributionCheck = null
                        scope.launch {
                            feedback = repository.completeTask(
                                taskId = currentTask.id,
                                taskTitle = currentTask.title,
                                theme = currentTask.theme.name,
                                isCorrect = option.isGood,
                                reward = currentTask.reward,
                            )
                            onTasksChanged()
                        }
                    },
                    onCheck = {
                        scope.launch {
                            val amounts = mapOf(
                                BudgetDirection.REQUIRED to requiredAmount,
                                BudgetDirection.OPTIONAL to optionalAmount,
                                BudgetDirection.SAVINGS to savingsAmount,
                            )
                            val check = repository.checkTaskDistribution(
                                amounts,
                                currentTask.sum,
                                currentTask.minimums,
                            )
                            distributionCheck = check
                            feedback = repository.completeTask(
                                taskId = currentTask.id,
                                taskTitle = currentTask.title,
                                theme = currentTask.theme.name,
                                isCorrect = check is DistributionCheck.Valid,
                                reward = currentTask.reward,
                            )
                            onTasksChanged()
                        }
                    },
                    onRetry = {
                        attempt++
                        chosenOptionId = null
                        feedback = null
                        distributionCheck = null
                        requiredAmount = 0L
                        optionalAmount = 0L
                        savingsAmount = 0L
                    },
                    onNextTask = { taskId -> currentTaskId = taskId },
                    onTryInGame = when (currentTask.theme) {
                        TaskTheme.BUDGET -> onOpenPlan
                        TaskTheme.SAVINGS -> onOpenSavings
                        TaskTheme.PAYMENTS -> onOpenShop
                    },
                    onBackToList = { currentTaskId = null },
                )
            }
        }
    }
}

@Composable
private fun tryInGameLabel(theme: TaskTheme): String = when (theme) {
    TaskTheme.BUDGET -> stringResource(R.string.tasks_try_plan)
    TaskTheme.SAVINGS -> stringResource(R.string.tasks_try_savings)
    TaskTheme.PAYMENTS -> stringResource(R.string.tasks_try_shop)
}

@Composable
private fun TaskCard(task: TaskContent, completed: Boolean, available: Boolean, onClick: () -> Unit) {
    ChunkySurface(
        onClick = onClick,
        enabled = available,
        modifier = Modifier.fillMaxWidth(),
        selected = completed,
        containerColor = if (completed) FinnyColors.SoftGreen else MaterialTheme.colorScheme.surface,
        edgeColor = if (completed) FinnyColors.SuccessEdge else FinnyColors.CardBorder,
        borderColor = if (completed) FinnyColors.Success else FinnyColors.CardBorder,
        minHeight = 64.dp,
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            when {
                completed -> StatusBadge(
                    text = stringResource(R.string.tasks_done_badge),
                    icon = Icons.Filled.Done,
                    kind = BadgeKind.POSITIVE,
                )
                !available -> StatusBadge(
                    text = stringResource(R.string.tasks_locked_week, task.week),
                    icon = Icons.Filled.Lock,
                    kind = BadgeKind.NEUTRAL,
                )
            }
        }
    }
}

@Composable
private fun TaskPlayView(
    task: TaskContent,
    options: List<TaskOption>,
    chosenOptionId: String?,
    feedback: TaskCompletionResult?,
    distributionCheck: DistributionCheck?,
    amounts: Map<BudgetDirection, Long>,
    nextTaskId: String?,
    onDistributeChange: (BudgetDirection, Long) -> Unit,
    onChooseOption: (TaskOption) -> Unit,
    onCheck: () -> Unit,
    onRetry: () -> Unit,
    onNextTask: (String) -> Unit,
    onTryInGame: () -> Unit,
    onBackToList: () -> Unit,
) {
    val locked = feedback is TaskCompletionResult.Success && feedback.isCorrect
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = task.title, style = MaterialTheme.typography.headlineSmall)
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = task.story, style = MaterialTheme.typography.bodyLarge)
            Text(text = task.question, style = MaterialTheme.typography.titleMedium)
        }
        when (task.type) {
            TaskType.CHOICE -> {
                options.forEach { option ->
                    OptionSurface(
                        selected = chosenOptionId == option.id,
                        enabled = !locked,
                        text = option.text,
                        onClick = { onChooseOption(option) },
                    )
                }
            }
            TaskType.DISTRIBUTE -> {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.tasks_distribute_sum, coinsAmount(task.sum)),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        BudgetDirection.entries.forEach { direction ->
                            AmountStepper(
                                label = directionTitle(direction),
                                caption = directionCaption(direction),
                                amount = amounts[direction] ?: 0L,
                                canIncrease = amounts.values.sum() < task.sum,
                                canDecrease = (amounts[direction] ?: 0L) > 0L,
                                onIncrease = { onDistributeChange(direction, (amounts[direction] ?: 0L) + 1L) },
                                onDecrease = { onDistributeChange(direction, (amounts[direction] ?: 0L) - 1L) },
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
                if (distributionCheck is DistributionCheck.Invalid) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Text(text = task.wrongExplanation, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                PrimaryButton(
                    text = stringResource(R.string.tasks_check_button),
                    onClick = onCheck,
                    enabled = !locked,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        val chosenOption = task.options.firstOrNull { it.id == chosenOptionId }
        feedback?.let { result ->
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (result) {
                        is TaskCompletionResult.Success -> {
                            if (result.isCorrect) {
                                StatusBadge(
                                    text = stringResource(R.string.tasks_correct_badge),
                                    icon = Icons.Filled.Check,
                                    kind = BadgeKind.POSITIVE,
                                )
                                if (result.reward > 0L) {
                                    Text(
                                        text = if (result.firstTry) {
                                            stringResource(R.string.tasks_reward_line, result.reward)
                                        } else {
                                            stringResource(R.string.tasks_retry_reward_line, result.reward, result.fullReward)
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                                if (result.moodDelta != 0) {
                                    Text(
                                        text = stringResource(R.string.shop_mood_line, result.moodDelta),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            } else {
                                StatusBadge(
                                    text = stringResource(R.string.tasks_try_again),
                                    icon = Icons.Filled.Warning,
                                    kind = BadgeKind.ATTENTION,
                                )
                                Text(
                                    text = stringResource(R.string.tasks_no_reward_line),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                        TaskCompletionResult.AlreadyCompleted -> {
                            StatusBadge(
                                text = stringResource(R.string.tasks_already_badge),
                                icon = Icons.Filled.Done,
                                kind = BadgeKind.NEUTRAL,
                            )
                            Text(
                                text = stringResource(R.string.tasks_already_rewarded),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        is TaskCompletionResult.Invalid -> {
                            Text(text = result.explanation, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    chosenOption?.let { option ->
                        Text(text = option.result, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (option.isGood) task.correctExplanation else task.wrongExplanation,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    val done = locked || result is TaskCompletionResult.AlreadyCompleted
                    if (done) {
                        Text(
                            text = stringResource(R.string.tasks_try_in_game_hint),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        PrimaryButton(
                            text = tryInGameLabel(task.theme),
                            onClick = onTryInGame,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        nextTaskId?.let { nextId ->
                            SecondaryButton(
                                text = stringResource(R.string.tasks_next),
                                onClick = { onNextTask(nextId) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        PrimaryButton(
                            text = stringResource(R.string.tasks_try_again),
                            onClick = onRetry,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        SecondaryButton(
            text = stringResource(R.string.tasks_back_to_list),
            onClick = onBackToList,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OptionSurface(selected: Boolean, enabled: Boolean, text: String, onClick: () -> Unit) {
    ChunkySurface(
        onClick = onClick,
        enabled = enabled,
        selected = selected,
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        edgeColor = if (selected) FinnyColors.PrimaryEdge else FinnyColors.CardBorder,
        borderColor = if (selected) MaterialTheme.colorScheme.primary else FinnyColors.CardBorder,
        borderWidth = if (selected) 3.dp else 2.dp,
        minHeight = 64.dp,
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

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