package ru.finny.petgame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.finny.petgame.R
import ru.finny.petgame.content.ContentLoader
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.ui.components.AppCard
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.BadgeKind
import ru.finny.petgame.ui.components.FinnyProgressBar
import ru.finny.petgame.ui.components.SectionAccent
import ru.finny.petgame.ui.components.SectionHeader
import ru.finny.petgame.ui.components.SectionTitle
import ru.finny.petgame.ui.components.StatusBadge
import ru.finny.petgame.ui.components.coinsAmount

@Composable
fun ProgressScreen(
    snapshot: GameSnapshot,
    onBack: () -> Unit,
    onHint: () -> Unit,
) {
    val profile = snapshot.profile
    val context = LocalContext.current
    var taskTitles by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(Unit) {
        taskTitles = withContext(Dispatchers.IO) {
            ContentLoader(context.assets).loadCatalog().tasks.associate { it.id to it.title }
        }
    }
    val rewardedTasks = snapshot.completedTasks.filter { it.isCorrect == true }
    val goalRow = snapshot.selectedGoalId?.let { id ->
        snapshot.savings.firstOrNull { it.goalId == id }
    }
    val last = snapshot.lastClosedPeriod

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
                accent = SectionAccent.PROGRESS,
                title = stringResource(R.string.section_progress),
                hint = stringResource(R.string.progress_header_hint),
                petSpecies = profile.petSpecies,
                petColorIndex = profile.petColor,
                petStage = profile.petStage,
            )

            AppCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle(
                    text = stringResource(R.string.progress_overview_title),
                    icon = Icons.Filled.Star,
                )
                Text(
                    text = stringResource(R.string.progress_stage_line, stageTitle(profile.petStage)),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(
                        R.string.progress_periods_line,
                        snapshot.closedPeriodCount,
                        profile.goodPeriods,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.progress_tasks_line, rewardedTasks.size),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle(
                    text = stringResource(R.string.progress_goal_title),
                    icon = Icons.Filled.Check,
                )
                when {
                    goalRow != null -> {
                        Text(text = goalRow.goalTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(
                                R.string.progress_goal_saved,
                                coinsAmount(goalRow.savedAmount),
                                coinsAmount(goalRow.goalCost),
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        FinnyProgressBar(
                            progress = if (goalRow.goalCost > 0L) {
                                (goalRow.savedAmount.toFloat() / goalRow.goalCost).coerceIn(0f, 1f)
                            } else {
                                0f
                            },
                            label = stringResource(
                                R.string.plan_fact_ratio,
                                goalRow.savedAmount,
                                goalRow.goalCost,
                            ),
                        )
                    }
                    snapshot.achievedGoalIds.isNotEmpty() && snapshot.selectedGoalId == null -> {
                        Text(
                            text = stringResource(R.string.progress_goal_pick_next),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    else -> {
                        Text(
                            text = stringResource(R.string.stat_goal_none),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle(
                    text = stringResource(R.string.progress_tasks_title),
                    icon = Icons.Filled.Check,
                )
                if (rewardedTasks.isEmpty()) {
                    Text(
                        text = stringResource(R.string.progress_tasks_empty),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    rewardedTasks.forEach { task ->
                        val title = taskTitles[task.taskId] ?: task.taskId
                        StatusBadge(
                            text = title,
                            icon = Icons.Filled.Check,
                            kind = BadgeKind.POSITIVE,
                        )
                        Text(
                            text = stringResource(
                                R.string.progress_task_reward,
                                themeLabel(task.theme),
                                task.reward,
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle(
                    text = stringResource(R.string.progress_last_period_title),
                    icon = Icons.Filled.Star,
                )
                if (last == null) {
                    Text(
                        text = stringResource(R.string.progress_last_period_empty),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.progress_last_period_num, last.periodIndex + 1),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    listOf(
                        BudgetDirection.REQUIRED,
                        BudgetDirection.OPTIONAL,
                        BudgetDirection.SAVINGS,
                    ).forEach { direction ->
                        val planAmount = last.plan[direction] ?: 0L
                        val factAmount = last.fact[direction] ?: 0L
                        Text(
                            text = stringResource(
                                R.string.progress_plan_fact_line,
                                directionLabel(direction),
                                planAmount,
                                factAmount,
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle(
                    text = stringResource(R.string.progress_glossary_title),
                    icon = Icons.Filled.Star,
                )
                GlossaryLine(R.string.glossary_plan_term, R.string.glossary_plan_body)
                GlossaryLine(R.string.glossary_fact_term, R.string.glossary_fact_body)
                GlossaryLine(R.string.glossary_required_term, R.string.glossary_required_body)
                GlossaryLine(R.string.glossary_optional_term, R.string.glossary_optional_body)
                GlossaryLine(R.string.glossary_savings_term, R.string.glossary_savings_body)
                GlossaryLine(R.string.glossary_goal_term, R.string.glossary_goal_body)
                GlossaryLine(R.string.glossary_period_term, R.string.glossary_period_body)
            }
        }
    }
}

@Composable
private fun themeLabel(theme: String): String = when (theme) {
    "BUDGET" -> stringResource(R.string.tasks_theme_budget)
    "SAVINGS" -> stringResource(R.string.tasks_theme_savings)
    "PAYMENTS" -> stringResource(R.string.tasks_theme_payments)
    else -> theme
}

@Composable
private fun GlossaryLine(termRes: Int, bodyRes: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = stringResource(termRes), style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(bodyRes), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun directionLabel(direction: BudgetDirection): String = when (direction) {
    BudgetDirection.REQUIRED -> stringResource(R.string.plan_direction_required)
    BudgetDirection.OPTIONAL -> stringResource(R.string.plan_direction_optional)
    BudgetDirection.SAVINGS -> stringResource(R.string.plan_direction_savings)
}
