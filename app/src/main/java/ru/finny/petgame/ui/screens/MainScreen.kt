package ru.finny.petgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.finny.petgame.R
import ru.finny.petgame.content.ContentCatalog
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.ui.components.ChunkySurface
import ru.finny.petgame.ui.components.CoinIcon
import ru.finny.petgame.ui.components.ForestBackground
import ru.finny.petgame.ui.components.ForestMeadow
import ru.finny.petgame.ui.components.LivePet
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.ScenePlate
import ru.finny.petgame.ui.components.SecondaryButton
import ru.finny.petgame.ui.components.SectionAccent
import ru.finny.petgame.ui.components.coinsAmount
import ru.finny.petgame.ui.components.sectionAccentColor
import ru.finny.petgame.ui.model.NextStep
import ru.finny.petgame.ui.model.nextStep
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
    onSettings: () -> Unit,
    seenSceneGoals: Set<String> = emptySet(),
    onSceneGoalsShown: (Set<String>) -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ForestBackground()
        if (snapshot == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@Box
        }
        val profile = snapshot.profile
        val periodIndex = snapshot.currentPeriod?.periodIndex ?: 0
        val week = catalog?.weekFor(periodIndex)
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatsRow(
                    balance = snapshot.balance,
                    savings = snapshot.savingsTotal,
                    stars = profile.growthStars,
                    onHint = onHint,
                    onSettings = onSettings,
                )
                WeekBanner(
                    weekNumber = periodIndex + 1,
                    theme = week?.theme,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))
                ScenePlate(
                    text = stringResource(R.string.main_name_stage, profile.petName, stageTitle(profile.petStage)),
                )
                Spacer(Modifier.height(6.dp))
                ScenePlate(
                    text = stringResource(R.string.main_pet_state, profile.mood, profile.saturation),
                    small = true,
                )
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    ForestMeadow(
                        petName = profile.petName,
                        achievedGoalIds = snapshot.achievedGoalIds,
                        selectedGoalId = snapshot.selectedGoalId,
                        savings = snapshot.savings,
                        pet = { petModifier ->
                            LivePet(
                                look = profile.toPetLook(),
                                stage = profile.petStage,
                                mood = profile.mood,
                                modifier = petModifier,
                            )
                        },
                        seenGoalIds = seenSceneGoals,
                        onGoalsShown = onSceneGoalsShown,
                        modifier = Modifier.fillMaxSize(),
                    )
                    WardrobeButton(
                        onClick = onWardrobe,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 12.dp),
                    )
                }
            }
            NextStepPanel(
                step = nextStep(snapshot, catalog),
                onPlan = onPlan,
                onShop = onShop,
                onSavings = onSavings,
                onOpenTask = onOpenTask,
                onClosePeriod = onClosePeriod,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            BottomNav(
                onPlan = onPlan,
                onTasks = onTasks,
                onShop = onShop,
                onSavings = onSavings,
                onProgress = onProgress,
                onAdult = onAdult,
            )
        }
    }
}

private val PlateShape = RoundedCornerShape(50)

@Composable
private fun StatsRow(
    balance: Long,
    savings: Long,
    stars: Int,
    onHint: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatChip(
            label = stringResource(R.string.stat_balance_label),
            value = balance.toString(),
            modifier = Modifier.weight(1f),
        ) { CoinIcon(Modifier.size(22.dp)) }
        StatChip(
            label = stringResource(R.string.stat_savings_label),
            value = savings.toString(),
            modifier = Modifier.weight(1f),
        ) { Icon(Icons.Filled.Favorite, null, tint = FinnyColors.Success, modifier = Modifier.size(22.dp)) }
        StatChip(
            label = stringResource(R.string.stat_stars_label),
            value = stars.toString(),
            modifier = Modifier.weight(1f),
        ) { Icon(Icons.Filled.Star, null, tint = FinnyColors.SplashBarFill, modifier = Modifier.size(24.dp)) }
        RoundIconButton(onClick = onSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.settings_title),
                tint = FinnyColors.SplashInk,
                modifier = Modifier.size(26.dp),
            )
        }
        RoundIconButton(onClick = onHint) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = stringResource(R.string.cd_hint),
                tint = FinnyColors.Primary,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun RoundIconButton(onClick: () -> Unit, icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(FinnyColors.SplashBarTrack.copy(alpha = 0.94f))
            .border(2.dp, FinnyColors.SplashInk, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { icon() }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val description = stringResource(R.string.main_stat_cd, label, value)
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(PlateShape)
            .background(FinnyColors.SplashBarTrack.copy(alpha = 0.94f))
            .border(2.dp, FinnyColors.SplashInk, PlateShape)
            .padding(horizontal = 10.dp)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        icon()
        Text(
            text = value,
            color = FinnyColors.SplashInk,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun WeekBanner(weekNumber: Int, theme: String?) {
    ChunkySurface(
        onClick = null,
        modifier = Modifier.fillMaxWidth(),
        containerColor = FinnyColors.Success,
        edgeColor = FinnyColors.SuccessEdge,
        borderColor = FinnyColors.SuccessEdge,
        borderWidth = 0.dp,
        minHeight = 64.dp,
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(modifier = Modifier.padding(horizontal = 6.dp)) {
            Text(
                text = stringResource(R.string.main_week_caps, weekNumber).uppercase(),
                color = FinnyColors.OnPrimary.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            if (theme != null) {
                Text(
                    text = theme,
                    color = FinnyColors.OnPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WardrobeButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(FinnyColors.SplashBarTrack.copy(alpha = 0.94f))
                .border(2.dp, FinnyColors.SplashInk, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Face, null, tint = FinnyColors.Tasks, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(2.dp))
        ScenePlate(text = stringResource(R.string.wardrobe_button), small = true)
    }
}

@Composable
private fun NextStepPanel(
    step: NextStep,
    onPlan: () -> Unit,
    onShop: () -> Unit,
    onSavings: () -> Unit,
    onOpenTask: (String) -> Unit,
    onClosePeriod: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FinnyColors.SplashBarTrack.copy(alpha = 0.95f))
            .border(2.dp, FinnyColors.SplashInk, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            color = FinnyColors.SplashInk,
            style = MaterialTheme.typography.bodyLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(text = button, onClick = action, modifier = Modifier.weight(1f))
            if (step !is NextStep.MakePlan && step !is NextStep.CloseWeek) {
                SecondaryButton(
                    text = stringResource(R.string.period_close_button),
                    onClick = onClosePeriod,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomNav(
    onPlan: () -> Unit,
    onTasks: () -> Unit,
    onShop: () -> Unit,
    onSavings: () -> Unit,
    onProgress: () -> Unit,
    onAdult: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().background(FinnyColors.SplashBarTrack)) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(FinnyColors.SplashInk))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 2.dp, vertical = 6.dp),
        ) {
            NavItem(stringResource(R.string.section_plan), Icons.Filled.List, SectionAccent.PLAN, onPlan)
            NavItem(stringResource(R.string.section_tasks), Icons.Filled.Edit, SectionAccent.TASKS, onTasks)
            NavItem(stringResource(R.string.section_shop), Icons.Filled.ShoppingCart, SectionAccent.SHOP, onShop)
            NavItem(stringResource(R.string.section_savings), Icons.Filled.Favorite, SectionAccent.SAVINGS, onSavings)
            NavItem(stringResource(R.string.section_progress), Icons.Filled.Star, SectionAccent.PROGRESS, onProgress)
            NavItem(stringResource(R.string.nav_adult), Icons.Filled.Lock, SectionAccent.ADULT, onAdult)
        }
    }
}

@Composable
private fun RowScope.NavItem(label: String, icon: ImageVector, accent: SectionAccent, onClick: () -> Unit) {
    val color: Color = sectionAccentColor(accent)
    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = FinnyColors.OnPrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = FinnyColors.SplashInk,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}
