package ru.finny.petgame.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.finny.petgame.R
import ru.finny.petgame.ui.components.sectionAccentColor
import ru.finny.petgame.ui.components.sectionAccentText
import ru.finny.petgame.ui.components.sectionPanel
import ru.finny.petgame.ui.components.sectionBackground
import ru.finny.petgame.ui.components.SectionAccent
import ru.finny.petgame.content.ContentCatalog
import ru.finny.petgame.content.ContentLoader
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.data.model.LedgerEntry
import ru.finny.petgame.data.model.LedgerKind
import ru.finny.petgame.data.repository.GameRepository
import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.FinnyProgressBar
import ru.finny.petgame.ui.components.PetSprite
import ru.finny.petgame.ui.components.coinsAmount
import ru.finny.petgame.ui.components.goalPicture
import ru.finny.petgame.ui.model.toPetLook
import ru.finny.petgame.ui.theme.FinnyColors

private enum class ProgressTab(val emoji: String, val labelRes: Int, val titleRes: Int) {
    GOAL("🎯", R.string.progress_tab_goal, R.string.progress_goal_title),
    TASKS("✅", R.string.progress_tab_tasks, R.string.progress_tasks_title),
    WEEK("📅", R.string.progress_tab_week, R.string.progress_last_period_title),
    COINS("🪙", R.string.progress_tab_coins, R.string.progress_ledger_title),
    WORDS("📖", R.string.progress_tab_words, R.string.progress_glossary_title),
}

@Composable
fun ProgressScreen(
    repository: GameRepository,
    snapshot: GameSnapshot,
    onBack: () -> Unit,
    onHint: () -> Unit,
) {
    val profile = snapshot.profile
    val context = LocalContext.current
    var catalog by remember { mutableStateOf<ContentCatalog?>(null) }
    var ledger by remember { mutableStateOf<List<LedgerEntry>?>(null) }
    var tab by rememberSaveable { mutableStateOf(ProgressTab.GOAL) }
    LaunchedEffect(Unit) {
        catalog = withContext(Dispatchers.IO) { ContentLoader(context.assets).loadCatalog() }
    }
    LaunchedEffect(snapshot) {
        ledger = repository.loadLedger()
    }
    val rewardedTasks = snapshot.completedTasks.filter { it.isCorrect == true }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.section_progress),
                showBack = true,
                onBack = onBack,
                onHint = onHint,
                accent = SectionAccent.PROGRESS,
            )
        },
        containerColor = sectionBackground(SectionAccent.PROGRESS),
        contentColor = FinnyColors.TextPrimary,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            // Финни и главные цифры всегда наверху — как в гардеробе и магазине
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(sectionPanel(SectionAccent.PROGRESS))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PetSprite(look = profile.toPetLook(), stage = profile.petStage, modifier = Modifier.size(104.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.progress_stage_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = FinnyColors.TextSecondary,
                    )
                    Text(
                        text = stageTitle(profile.petStage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = FinnyColors.TextPrimary,
                    )
                    StatPill("📅 " + stringResource(R.string.progress_stat_weeks, snapshot.closedPeriodCount))
                    StatPill("⭐ " + stringResource(R.string.progress_stat_stars, profile.goodPeriods))
                    StatPill("✅ " + stringResource(R.string.progress_stat_tasks, rewardedTasks.size))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(sectionPanel(SectionAccent.PROGRESS))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ProgressTab.entries.forEach { entry ->
                    TabTile(
                        emoji = entry.emoji,
                        label = stringResource(entry.labelRes),
                        selected = entry == tab,
                        onClick = { tab = entry },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Text(
                text = stringResource(tab.titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = FinnyColors.TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (tab) {
                    ProgressTab.GOAL -> GoalTab(snapshot, catalog)
                    ProgressTab.TASKS -> TasksTab(snapshot, catalog)
                    ProgressTab.WEEK -> WeekTab(snapshot)
                    ProgressTab.COINS -> CoinsTab(ledger)
                    ProgressTab.WORDS -> WordsTab()
                }
            }
        }
    }
}

// ---------- Мечта ----------

@Composable
private fun GoalTab(snapshot: GameSnapshot, catalog: ContentCatalog?) {
    val goalRow = snapshot.selectedGoalId?.let { id -> snapshot.savings.firstOrNull { it.goalId == id } }
    if (goalRow != null) {
        Tile {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GoalPicture(goalId = goalRow.goalId, size = 96)
                Text(
                    text = goalRow.goalTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = FinnyColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                FinnyProgressBar(
                    progress = if (goalRow.goalCost > 0L) {
                        (goalRow.savedAmount.toFloat() / goalRow.goalCost).coerceIn(0f, 1f)
                    } else {
                        0f
                    },
                    label = stringResource(R.string.plan_fact_ratio, goalRow.savedAmount, goalRow.goalCost),
                )
                Text(
                    text = stringResource(
                        R.string.progress_goal_left,
                        coinsAmount((goalRow.goalCost - goalRow.savedAmount).coerceAtLeast(0L)),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FinnyColors.TextSecondary,
                )
            }
        }
    } else {
        EmptyState(
            emoji = "🎯",
            text = stringResource(
                if (snapshot.achievedGoalIds.isNotEmpty()) R.string.progress_goal_pick_next else R.string.stat_goal_none,
            ),
        )
    }

    val achieved = catalog?.goals.orEmpty().filter { it.id in snapshot.achievedGoalIds }
    if (achieved.isNotEmpty()) {
        Text(
            text = stringResource(R.string.progress_goal_done_title),
            style = MaterialTheme.typography.titleMedium,
            color = FinnyColors.TextPrimary,
            modifier = Modifier.padding(top = 6.dp),
        )
        achieved.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { goal ->
                    Tile(modifier = Modifier.weight(1f), borderColor = FinnyColors.Success) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            GoalPicture(goalId = goal.id, size = 64)
                            Text(
                                text = goal.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                minLines = 2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Check, null, tint = FinnyColors.Success, modifier = Modifier.size(16.dp))
                                Text(
                                    text = coinsAmount(goal.cost),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = FinnyColors.Success,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }
                    }
                }
                if (pair.size == 1) Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GoalPicture(goalId: String, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (FinnyColors.isDark()) MaterialTheme.colorScheme.surfaceVariant else FinnyColors.SoftGreen),
        contentAlignment = Alignment.Center,
    ) {
        val picture = goalPicture(goalId)
        if (picture != null) {
            Image(
                painter = painterResource(picture),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding((size / 8).dp),
            )
        } else {
            Text(text = "🎁", fontSize = (size / 2).sp)
        }
    }
}

// ---------- Задания ----------

@Composable
private fun TasksTab(snapshot: GameSnapshot, catalog: ContentCatalog?) {
    val rewarded = snapshot.completedTasks.filter { it.isCorrect == true }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            Triple("📋", "BUDGET", R.string.progress_theme_budget_short),
            Triple("🐷", "SAVINGS", R.string.progress_theme_savings_short),
            Triple("🛒", "PAYMENTS", R.string.progress_theme_payments_short),
        ).forEach { (emoji, theme, labelRes) ->
            Tile(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = emoji, fontSize = 28.sp)
                    Text(
                        text = rewarded.count { it.theme == theme }.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = FinnyColors.Tasks,
                    )
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = FinnyColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
    if (rewarded.isEmpty()) {
        EmptyState(emoji = "✏️", text = stringResource(R.string.progress_tasks_empty))
        return
    }
    Text(
        text = stringResource(R.string.progress_tasks_recent),
        style = MaterialTheme.typography.titleMedium,
        color = FinnyColors.TextPrimary,
        modifier = Modifier.padding(top = 6.dp),
    )
    val titles = remember(catalog) { catalog?.tasks.orEmpty().associate { it.id to it.title } }
    rewarded.sortedByDescending { it.completedAt }.forEach { task ->
        ListRow(
            emoji = themeEmoji(task.theme),
            title = titles[task.taskId] ?: task.taskId,
            caption = themeLabel(task.theme),
            trailing = "+${task.reward}",
            trailingColor = FinnyColors.Success,
        )
    }
}

// ---------- Неделя ----------

@Composable
private fun WeekTab(snapshot: GameSnapshot) {
    val last = snapshot.lastClosedPeriod
    if (last == null) {
        EmptyState(emoji = "📅", text = stringResource(R.string.progress_last_period_empty))
        return
    }
    Text(
        text = stringResource(R.string.progress_last_period_num, last.periodIndex + 1),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = FinnyColors.TextPrimary,
    )
    BudgetDirection.entries.forEach { direction ->
        val plan = last.plan[direction] ?: 0L
        val fact = last.fact[direction] ?: 0L
        val ok = if (direction == BudgetDirection.SAVINGS) fact >= plan else fact <= plan
        val status = when {
            ok -> stringResource(R.string.progress_week_ok)
            direction == BudgetDirection.SAVINGS -> stringResource(R.string.progress_week_saved_less)
            else -> stringResource(R.string.progress_week_over)
        }
        Tile(borderColor = if (ok) MaterialTheme.colorScheme.outlineVariant else FinnyColors.Optional) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiCircle(emoji = directionEmoji(direction), color = directionColor(direction))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = directionLabel(direction),
                        style = MaterialTheme.typography.titleMedium,
                        color = FinnyColors.TextPrimary,
                    )
                    FinnyProgressBar(
                        progress = if (plan > 0L) (fact.toFloat() / plan).coerceIn(0f, 1f) else if (fact > 0L) 1f else 0f,
                        label = stringResource(R.string.progress_week_in_plan, plan, fact),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (ok) Icons.Filled.Check else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (ok) FinnyColors.Success else FinnyColors.BadgeAttentionContent,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (ok) FinnyColors.Success else FinnyColors.BadgeAttentionContent,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---------- Монеты ----------

@Composable
private fun CoinsTab(ledger: List<LedgerEntry>?) {
    if (ledger.isNullOrEmpty()) {
        EmptyState(emoji = "🪙", text = stringResource(R.string.progress_ledger_empty))
        return
    }
    ledger.groupBy { it.periodIndex }
        .toSortedMap(compareByDescending { it })
        .entries
        .take(LEDGER_WEEKS_SHOWN)
        .forEach { (periodIndex, weekEntries) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.main_period_line, periodIndex + 1),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinnyColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                StatPill(
                    text = stringResource(
                        R.string.progress_ledger_in,
                        coinsAmount(weekEntries.filter { it.amount > 0 }.sumOf { it.amount }),
                    ),
                    color = FinnyColors.SoftGreen,
                )
                StatPill(
                    text = stringResource(
                        R.string.progress_ledger_out,
                        coinsAmount(-weekEntries.filter { it.amount < 0 }.sumOf { it.amount }),
                    ),
                    color = FinnyColors.SoftOrange,
                )
            }
            weekEntries.forEach { entry ->
                ListRow(
                    emoji = when (entry.kind) {
                        LedgerKind.INCOME -> "💰"
                        LedgerKind.PURCHASE -> "🛒"
                        LedgerKind.TO_SAVINGS -> "🐷"
                        LedgerKind.FROM_SAVINGS -> "↩️"
                    },
                    title = entry.title,
                    caption = null,
                    trailing = if (entry.amount >= 0) "+${entry.amount}" else "−${-entry.amount}",
                    trailingColor = if (entry.amount >= 0) FinnyColors.Success else FinnyColors.Optional,
                )
            }
        }
}

private const val LEDGER_WEEKS_SHOWN = 3

// ---------- Слова ----------

private data class Word(val emoji: String, val termRes: Int, val bodyRes: Int)

private val WORDS = listOf(
    Word("💰", R.string.glossary_income_term, R.string.glossary_income_body),
    Word("🛍️", R.string.glossary_expense_term, R.string.glossary_expense_body),
    Word("👛", R.string.glossary_remainder_term, R.string.glossary_remainder_body),
    Word("🔄", R.string.glossary_change_term, R.string.glossary_change_body),
    Word("📋", R.string.glossary_plan_term, R.string.glossary_plan_body),
    Word("📊", R.string.glossary_fact_term, R.string.glossary_fact_body),
    Word("🥣", R.string.glossary_required_term, R.string.glossary_required_body),
    Word("🎈", R.string.glossary_optional_term, R.string.glossary_optional_body),
    Word("🐷", R.string.glossary_savings_term, R.string.glossary_savings_body),
    Word("🎯", R.string.glossary_goal_term, R.string.glossary_goal_body),
    Word("📅", R.string.glossary_period_term, R.string.glossary_period_body),
)

@Composable
private fun WordsTab() {
    var opened by rememberSaveable { mutableStateOf<Int?>(null) }
    Text(
        text = stringResource(R.string.progress_words_hint),
        style = MaterialTheme.typography.bodyLarge,
        color = FinnyColors.TextSecondary,
    )
    WORDS.chunked(2).forEach { pair ->
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            pair.forEach { word ->
                val isOpen = opened == word.termRes
                WordCard(
                    word = word,
                    open = isOpen,
                    onClick = { opened = if (isOpen) null else word.termRes },
                    modifier = Modifier.weight(1f),
                )
            }
            if (pair.size == 1) Box(modifier = Modifier.weight(1f))
        }
    }
}

/** Карточка-подсказка: сверху слово, по нажатию — объяснение, как карточки для запоминания. */
@Composable
private fun WordCard(word: Word, open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = 150.dp)
            .clip(shape)
            .background(if (open) sectionBackground(SectionAccent.PROGRESS) else MaterialTheme.colorScheme.surface)
            .border(if (open) 3.dp else 2.dp, if (open) sectionAccentColor(SectionAccent.PROGRESS) else MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (open) {
            Text(
                text = stringResource(word.termRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = sectionAccentText(SectionAccent.PROGRESS),
            )
            Text(
                text = stringResource(word.bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = FinnyColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            Text(text = word.emoji, fontSize = 40.sp)
            Text(
                text = stringResource(word.termRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FinnyColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

// ---------- Общие кусочки ----------

@Composable
private fun TabTile(emoji: String, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(if (selected) sectionBackground(SectionAccent.PROGRESS) else MaterialTheme.colorScheme.surface)
            .border(
                width = if (selected) 3.dp else 2.dp,
                color = if (selected) sectionAccentColor(SectionAccent.PROGRESS) else MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = emoji, fontSize = 24.sp)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) sectionAccentText(SectionAccent.PROGRESS) else FinnyColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun StatPill(text: String, color: Color = MaterialTheme.colorScheme.surface) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = FinnyColors.TextPrimary)
    }
}

@Composable
private fun Tile(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, borderColor, shape)
            .padding(12.dp),
    ) {
        content()
    }
}

@Composable
private fun ListRow(emoji: String, title: String, caption: String?, trailing: String, trailingColor: Color) {
    Tile {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 24.sp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = FinnyColors.TextPrimary,
                )
                caption?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = FinnyColors.TextSecondary)
                }
            }
            Text(
                text = trailing,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = trailingColor,
            )
        }
    }
}

@Composable
private fun EmojiCircle(emoji: String, color: Color) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = 26.sp)
    }
}

@Composable
private fun EmptyState(emoji: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = emoji, fontSize = 48.sp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = FinnyColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

private fun themeEmoji(theme: String): String = when (theme) {
    "BUDGET" -> "📋"
    "SAVINGS" -> "🐷"
    else -> "🛒"
}

@Composable
private fun themeLabel(theme: String): String = when (theme) {
    "BUDGET" -> stringResource(R.string.tasks_theme_budget)
    "SAVINGS" -> stringResource(R.string.tasks_theme_savings)
    "PAYMENTS" -> stringResource(R.string.tasks_theme_payments)
    else -> theme
}

private fun directionEmoji(direction: BudgetDirection): String = when (direction) {
    BudgetDirection.REQUIRED -> "🥣"
    BudgetDirection.OPTIONAL -> "🎈"
    BudgetDirection.SAVINGS -> "🐷"
}

private fun directionColor(direction: BudgetDirection): Color = when (direction) {
    BudgetDirection.REQUIRED -> FinnyColors.SoftGreen
    BudgetDirection.OPTIONAL -> FinnyColors.SoftOrange
    BudgetDirection.SAVINGS -> FinnyColors.SoftPurple
}

@Composable
private fun directionLabel(direction: BudgetDirection): String = when (direction) {
    BudgetDirection.REQUIRED -> stringResource(R.string.plan_direction_required)
    BudgetDirection.OPTIONAL -> stringResource(R.string.plan_direction_optional)
    BudgetDirection.SAVINGS -> stringResource(R.string.plan_direction_savings)
}
