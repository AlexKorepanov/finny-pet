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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.AmountStepper
import ru.finny.petgame.ui.components.BadgeKind
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SectionAccent
import ru.finny.petgame.ui.components.SectionHeader
import ru.finny.petgame.ui.components.SectionTitle
import ru.finny.petgame.ui.components.SecondaryButton
import ru.finny.petgame.ui.components.StatusBadge
import ru.finny.petgame.ui.components.coinsAmount

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
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tasks by remember { mutableStateOf<List<TaskContent>?>(null) }
    var currentTaskId by rememberSaveable { mutableStateOf(focusedTaskId) }
    var chosenOptionId by remember(currentTaskId) { mutableStateOf<String?>(null) }
    var feedback by remember(currentTaskId) { mutableStateOf<TaskCompletionResult?>(null) }
    var distributionCheck by remember(currentTaskId) { mutableStateOf<DistributionCheck?>(null) }
    var requiredAmount by remember(currentTaskId) { mutableLongStateOf(0L) }
    var optionalAmount by remember(currentTaskId) { mutableLongStateOf(0L) }
    var savingsAmount by remember(currentTaskId) { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        tasks = withContext(Dispatchers.IO) {
            ContentLoader(context.assets).loadCatalog().tasks
        }
    }

    val completedIds = snapshot.completedTasks.filter { it.isCorrect == true }.map { it.taskId }.toSet()
    val taskList = tasks
    val currentTask = taskList?.firstOrNull { it.id == currentTaskId }
    val orderById = taskList?.withIndex()?.associate { it.value.id to it.index }.orEmpty()
    val nextIncompleteTask = taskList
        ?.filter { it.id != currentTaskId && it.id !in completedIds }
        ?.firstOrNull()

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
                petSpecies = snapshot.profile.petSpecies,
                petColorIndex = snapshot.profile.petColor,
            )
            if (taskList == null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (currentTask == null) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.tasks_all_open),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = snapshot.profile.allTasksOpen,
                            onCheckedChange = { open ->
                                scope.launch {
                                    repository.setAllTasksOpen(open)
                                    onTasksChanged()
                                }
                            },
                        )
                    }
                }
                TaskTheme.entries.forEach { theme ->
                    SectionTitle(
                        text = themeTitle(theme),
                        accent = SectionAccent.TASKS,
                    )
                    taskList.filter { it.theme == theme }.forEach { task ->
                        val globalIndex = orderById[task.id] ?: 0
                        val available = snapshot.profile.allTasksOpen ||
                            taskList.take(globalIndex).all { it.id in completedIds }
                        TaskCard(
                            task = task,
                            completed = task.id in completedIds,
                            available = available,
                            onClick = { currentTaskId = task.id },
                        )
                    }
                }
            } else {
                TaskPlayView(
                    task = currentTask,
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
                        chosenOptionId = null
                        feedback = null
                        distributionCheck = null
                        requiredAmount = 0L
                        optionalAmount = 0L
                        savingsAmount = 0L
                    },
                    onNextTask = { taskId -> currentTaskId = taskId },
                    onOpenPlan = onOpenPlan,
                    onOpenShop = onOpenShop,
                    onBackToList = { currentTaskId = null },
                )
            }
        }
    }
}

@Composable
private fun themeTitle(theme: TaskTheme): String = when (theme) {
    TaskTheme.BUDGET -> stringResource(R.string.tasks_theme_budget)
    TaskTheme.SAVINGS -> stringResource(R.string.tasks_theme_savings)
    TaskTheme.PAYMENTS -> stringResource(R.string.tasks_theme_payments)
}

@Composable
private fun TaskCard(task: TaskContent, completed: Boolean, available: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = available,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
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
                    text = stringResource(R.string.tasks_locked_badge),
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
    onOpenPlan: () -> Unit,
    onOpenShop: () -> Unit,
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
                task.options.forEach { option ->
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
                                        text = stringResource(R.string.tasks_reward_line, result.reward),
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
                    if (locked) {
                        nextTaskId?.let { nextId ->
                            PrimaryButton(
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton(
                            text = stringResource(R.string.tasks_to_plan),
                            onClick = onOpenPlan,
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            text = stringResource(R.string.tasks_to_shop),
                            onClick = onOpenShop,
                            modifier = Modifier.weight(1f),
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
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(12.dp),
        )
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