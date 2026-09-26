package ru.finny.petgame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.finny.petgame.R
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.data.repository.GameRepository
import ru.finny.petgame.ui.components.AppCard
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.BadgeKind
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SecondaryButton
import ru.finny.petgame.ui.components.coinsAmount
import ru.finny.petgame.ui.components.SectionAccent
import ru.finny.petgame.ui.components.SectionHeader
import ru.finny.petgame.ui.model.toPetLook
import ru.finny.petgame.ui.components.SectionTitle
import ru.finny.petgame.ui.components.StatusBadge
import ru.finny.petgame.ui.theme.FinnyColors
import kotlin.random.Random

@Composable
fun AdultScreen(
    repository: GameRepository,
    snapshot: GameSnapshot,
    accessoryPurse: Long,
    onBack: () -> Unit,
    onHint: () -> Unit,
    onDemoChanged: () -> Unit,
    onProfileDeleted: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var unlocked by rememberSaveable { mutableStateOf(false) }
    var answerText by rememberSaveable { mutableStateOf("") }
    var a by rememberSaveable { mutableIntStateOf(Random.nextInt(2, 10)) }
    var b by rememberSaveable { mutableIntStateOf(Random.nextInt(2, 10)) }
    var gateError by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var coinsNote by remember { mutableStateOf<CoinsNote>(CoinsNote.None) }

    val themesDone = snapshot.completedTasks
        .filter { it.isCorrect == true }
        .map { it.theme }
        .toSet()
    val themeStats = themeStats(snapshot)

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
                accent = SectionAccent.ADULT,
                title = stringResource(R.string.section_adult),
                hint = stringResource(R.string.adult_header_hint),
                petLook = snapshot.profile.toPetLook(),
                petStage = snapshot.profile.petStage,
            )

            if (!unlocked) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusBadge(
                            text = stringResource(R.string.adult_gate_title),
                            icon = Icons.Filled.Lock,
                            kind = BadgeKind.NEUTRAL,
                        )
                        Text(
                            text = stringResource(R.string.adult_gate_text, a, b),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        OutlinedTextField(
                            value = answerText,
                            onValueChange = {
                                answerText = it.filter { ch -> ch.isDigit() }.take(3)
                                gateError = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text(text = stringResource(R.string.adult_gate_answer)) },
                        )
                        if (gateError) {
                            StatusBadge(
                                text = stringResource(R.string.adult_gate_wrong),
                                icon = Icons.Filled.Warning,
                                kind = BadgeKind.ATTENTION,
                            )
                        }
                        PrimaryButton(
                            text = stringResource(R.string.adult_gate_open),
                            onClick = {
                                val value = answerText.toIntOrNull()
                                if (value == a + b) {
                                    unlocked = true
                                    gateError = false
                                } else {
                                    gateError = true
                                    a = Random.nextInt(2, 10)
                                    b = Random.nextInt(2, 10)
                                    answerText = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = FinnyColors.Adult,
                            edgeColor = FinnyColors.Adult.copy(alpha = 0.75f),
                        )
                    }
                }
            } else {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(
                        text = stringResource(R.string.adult_goals_title),
                        icon = Icons.Filled.Star,
                    )
                    Text(
                        text = stringResource(R.string.adult_goals_body),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(
                        text = stringResource(R.string.adult_progress_title),
                        icon = Icons.Filled.Check,
                    )
                    Text(
                        text = stringResource(
                            R.string.adult_progress_stage,
                            stageTitle(snapshot.profile.petStage),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(
                            R.string.adult_progress_periods,
                            snapshot.closedPeriodCount,
                            snapshot.profile.goodPeriods,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(
                            R.string.adult_progress_tasks,
                            snapshot.completedTasks.count { it.isCorrect == true },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.adult_themes_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    ThemeLine(R.string.tasks_theme_budget, "BUDGET" in themesDone, themeStats["BUDGET"])
                    ThemeLine(R.string.tasks_theme_savings, "SAVINGS" in themesDone, themeStats["SAVINGS"])
                    ThemeLine(R.string.tasks_theme_payments, "PAYMENTS" in themesDone, themeStats["PAYMENTS"])
                    Text(
                        text = stringResource(R.string.adult_stats_note),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.adult_demo_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.adult_demo_body),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        Switch(
                            checked = snapshot.profile.allTasksOpen,
                            onCheckedChange = { open ->
                                scope.launch {
                                    repository.setAllTasksOpen(open)
                                    onDemoChanged()
                                }
                            },
                        )
                    }
                    Text(
                        text = stringResource(R.string.adult_demo_coins_body),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    PrimaryButton(
                        text = stringResource(R.string.adult_demo_coins_button),
                        onClick = {
                            scope.launch {
                                val added = repository.grantDemoCoins(accessoryPurse)
                                val balance = repository.loadSnapshot()?.balance ?: snapshot.balance
                                coinsNote = if (added == 0L) CoinsNote.Enough else CoinsNote.Granted(balance)
                                onDemoChanged()
                            }
                        },
                        enabled = accessoryPurse > 0L,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    when (val note = coinsNote) {
                        CoinsNote.None -> Unit
                        CoinsNote.Enough -> Text(
                            text = stringResource(R.string.adult_demo_coins_enough),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        is CoinsNote.Granted -> Text(
                            text = stringResource(R.string.adult_demo_coins_done, coinsAmount(note.balance)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(
                        text = stringResource(R.string.adult_profile_title),
                        icon = Icons.Filled.Lock,
                    )
                    Text(
                        text = stringResource(R.string.adult_profile_local),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.adult_reset_hint),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    PrimaryButton(
                        text = stringResource(R.string.adult_reset_button),
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = FinnyColors.Adult,
                        edgeColor = FinnyColors.Adult.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(text = stringResource(R.string.adult_reset_confirm_title)) },
            text = { Text(text = stringResource(R.string.adult_reset_confirm_text)) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.adult_reset_confirm),
                        onClick = {
                            scope.launch {
                                if (repository.deleteCurrentProfile()) {
                                    showDeleteConfirm = false
                                    onProfileDeleted()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        containerColor = FinnyColors.Adult,
                        edgeColor = FinnyColors.Adult.copy(alpha = 0.75f),
                    )
                    SecondaryButton(
                        text = stringResource(R.string.adult_reset_cancel),
                        onClick = { showDeleteConfirm = false },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }
}

private sealed interface CoinsNote {
    data object None : CoinsNote
    data object Enough : CoinsNote
    data class Granted(val balance: Long) : CoinsNote
}

@Composable
private fun ThemeLine(labelRes: Int, done: Boolean, stats: ThemeStats?) {
    StatusBadge(
        text = stringResource(labelRes),
        icon = if (done) Icons.Filled.Check else Icons.Filled.Lock,
        kind = if (done) BadgeKind.POSITIVE else BadgeKind.NEUTRAL,
    )
    Text(
        text = stringResource(R.string.adult_theme_stats, stats?.solved ?: 0, stats?.firstTry ?: 0),
        style = MaterialTheme.typography.bodyLarge,
    )
}

private data class ThemeStats(val solved: Int, val firstTry: Int)

private fun themeStats(snapshot: GameSnapshot): Map<String, ThemeStats> =
    snapshot.completedTasks
        .groupBy { it.theme }
        .mapValues { (_, rows) ->
            val byTask = rows.groupBy { it.taskId }.values.map { attempts -> attempts.sortedBy { it.id } }
            ThemeStats(
                solved = byTask.count { attempts -> attempts.any { it.isCorrect == true } },
                firstTry = byTask.count { attempts -> attempts.first().isCorrect == true },
            )
        }
