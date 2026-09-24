package ru.finny.petgame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.content.ContentCatalog
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.economy.EconomyEngine
import ru.finny.petgame.ui.components.AppCard
import ru.finny.petgame.ui.components.SecondaryButton
import ru.finny.petgame.ui.components.coinsAmount
import ru.finny.petgame.ui.model.NextStep
import ru.finny.petgame.ui.model.nextOpenTask
import ru.finny.petgame.ui.model.nextStep
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.BadgeKind
import ru.finny.petgame.ui.components.ChunkySurface
import ru.finny.petgame.ui.components.FinnyProgressBar
import ru.finny.petgame.ui.components.PetSprite
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SectionAccent
import ru.finny.petgame.ui.components.SectionTitle
import ru.finny.petgame.ui.components.StatusBadge
import ru.finny.petgame.ui.components.sectionAccentColor
import ru.finny.petgame.ui.components.sectionAccentEdge
import ru.finny.petgame.ui.model.toPetLook
import ru.finny.petgame.ui.theme.FinnyColors

@Composable
fun MainScreen(
    snapshot: GameSnapshot?,
    catalog: ContentCatalog?,
    onHint: () -> Unit,
    onPlan: () -> Unit,
    onShop: () -> Unit,
    onSavings: () -> Unit,
    onTasks: () -> Unit,
    onProgress: () -> Unit,
    onAdult: () -> Unit,
    onOpenTask: (String) -> Unit,
    onClosePeriod: () -> Unit,
    onWardrobe: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_name),
                showBack = false,
                onBack = {},
                onHint = onHint,
            )
        },
    ) { padding ->
        if (snapshot == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val profile = snapshot.profile
            val periodIndex = snapshot.currentPeriod?.periodIndex ?: 0
            val week = catalog?.weekFor(periodIndex)
            val starsLeft = EconomyEngine().starsToNextStage(profile.growthStars)
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        PetSprite(
                            look = profile.toPetLook(),
                            stage = profile.petStage,
                            modifier = Modifier.size(140.dp),
                        )
                        Text(
                            text = stringResource(R.string.main_greeting),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = profile.petName,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        SecondaryButton(
                            text = stringResource(R.string.wardrobe_button),
                            onClick = onWardrobe,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = if (week != null) {
                                stringResource(R.string.main_week_theme_line, periodIndex + 1, week.theme)
                            } else {
                                stringResource(R.string.main_period_line, periodIndex + 1)
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(
                                R.string.main_stage_line,
                                stageTitle(profile.petStage),
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (starsLeft != null) {
                                stringResource(R.string.main_stars_line, profile.growthStars, starsLeft)
                            } else {
                                stringResource(R.string.main_stars_max_line, profile.growthStars)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                NextStepCard(
                    step = nextStep(snapshot, catalog),
                    onPlan = onPlan,
                    onShop = onShop,
                    onSavings = onSavings,
                    onOpenTask = onOpenTask,
                    onClosePeriod = onClosePeriod,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        title = stringResource(R.string.stat_balance_label),
                        value = snapshot.balance.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = stringResource(R.string.stat_savings_label),
                        value = snapshot.savingsTotal.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(
                        text = stringResource(R.string.stat_goal_label),
                        icon = Icons.Filled.Done,
                    )
                    Text(
                        text = snapshot.selectedGoalTitle ?: stringResource(R.string.stat_goal_none),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(
                        text = stringResource(R.string.pet_state_title),
                        icon = Icons.Filled.Face,
                    )
                    MoodRow(label = stringResource(R.string.pet_state_mood), value = profile.mood)
                    MoodRow(label = stringResource(R.string.pet_state_saturation), value = profile.saturation)
                }
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(
                        text = stringResource(R.string.active_task_title),
                        icon = Icons.Filled.PlayArrow,
                    )
                    val nextTask = nextOpenTask(snapshot, catalog)
                    val allDone = catalog != null &&
                        catalog.tasks.all { task -> snapshot.completedTasks.any { it.taskId == task.id && it.isCorrect == true } }
                    when {
                        catalog == null -> {
                            Text(text = stringResource(R.string.active_task_stub), style = MaterialTheme.typography.bodyLarge)
                        }
                        nextTask != null -> {
                            Text(text = nextTask.title, style = MaterialTheme.typography.bodyLarge)
                            PrimaryButton(
                                text = stringResource(R.string.tasks_start),
                                onClick = { onOpenTask(nextTask.id) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        allDone -> {
                            StatusBadge(
                                text = stringResource(R.string.tasks_all_done),
                                icon = Icons.Filled.Done,
                                kind = BadgeKind.POSITIVE,
                            )
                        }
                        else -> {
                            StatusBadge(
                                text = stringResource(R.string.tasks_week_done),
                                icon = Icons.Filled.Done,
                                kind = BadgeKind.POSITIVE,
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTile(
                        label = stringResource(R.string.section_plan),
                        icon = Icons.Filled.List,
                        accent = SectionAccent.PLAN,
                        onClick = onPlan,
                        modifier = Modifier.weight(1f),
                    )
                    SectionTile(
                        label = stringResource(R.string.section_tasks),
                        icon = Icons.Filled.Edit,
                        accent = SectionAccent.TASKS,
                        onClick = onTasks,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTile(
                        label = stringResource(R.string.section_shop),
                        icon = Icons.Filled.ShoppingCart,
                        accent = SectionAccent.SHOP,
                        onClick = onShop,
                        modifier = Modifier.weight(1f),
                    )
                    SectionTile(
                        label = stringResource(R.string.section_savings),
                        icon = Icons.Filled.Favorite,
                        accent = SectionAccent.SAVINGS,
                        onClick = onSavings,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTile(
                        label = stringResource(R.string.section_progress),
                        icon = Icons.Filled.Star,
                        accent = SectionAccent.PROGRESS,
                        onClick = onProgress,
                        modifier = Modifier.weight(1f),
                    )
                    SectionTile(
                        label = stringResource(R.string.section_adult),
                        icon = Icons.Filled.Lock,
                        accent = SectionAccent.ADULT,
                        onClick = onAdult,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NextStepCard(
    step: NextStep,
    onPlan: () -> Unit,
    onShop: () -> Unit,
    onSavings: () -> Unit,
    onOpenTask: (String) -> Unit,
    onClosePeriod: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(
            text = stringResource(R.string.next_step_title),
            icon = Icons.Filled.PlayArrow,
        )
        val (text, button, action) = when (step) {
            NextStep.MakePlan -> Triple(
                stringResource(R.string.next_step_plan),
                stringResource(R.string.plan_make),
                onPlan,
            )
            is NextStep.BuyNeeds -> Triple(
                stringResource(R.string.next_step_needs, step.titles.joinToString()),
                stringResource(R.string.tasks_to_shop),
                onShop,
            )
            is NextStep.Save -> Triple(
                stringResource(R.string.next_step_save, coinsAmount(step.amount)),
                stringResource(R.string.next_step_save_button),
                onSavings,
            )
            is NextStep.SolveTask -> Triple(
                stringResource(R.string.next_step_task, step.task.title),
                stringResource(R.string.tasks_start),
                { onOpenTask(step.task.id) },
            )
            NextStep.CloseWeek -> Triple(
                stringResource(R.string.next_step_close),
                stringResource(R.string.period_close_button),
                onClosePeriod,
            )
        }
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
        PrimaryButton(text = button, onClick = action, modifier = Modifier.fillMaxWidth())
        if (step !is NextStep.MakePlan && step !is NextStep.CloseWeek) {
            SecondaryButton(
                text = stringResource(R.string.period_close_button),
                onClick = onClosePeriod,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        SectionTitle(text = title, icon = Icons.Filled.Star)
        Text(text = value, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun MoodRow(label: String, value: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        FinnyProgressBar(
            progress = value.coerceIn(0, 100) / 100f,
            label = value.toString(),
        )
    }
}

@Composable
private fun SectionTile(
    label: String,
    icon: ImageVector,
    accent: SectionAccent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ChunkySurface(
        onClick = onClick,
        modifier = modifier,
        containerColor = sectionAccentColor(accent),
        edgeColor = sectionAccentEdge(accent),
        borderColor = sectionAccentEdge(accent),
        borderWidth = 0.dp,
        minHeight = 108.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FinnyColors.OnPrimary,
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = label,
                color = FinnyColors.OnPrimary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
