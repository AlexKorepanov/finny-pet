package ru.finny.petgame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import ru.finny.petgame.ui.components.goalPicture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import ru.finny.petgame.content.model.SavingsGoalContent
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.data.model.GoalAchieveResult
import ru.finny.petgame.data.repository.GameRepository
import ru.finny.petgame.economy.model.DepositResult
import ru.finny.petgame.economy.model.GoalEta
import ru.finny.petgame.economy.model.WithdrawResult
import ru.finny.petgame.ui.components.AppCard
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.AmountStepper
import ru.finny.petgame.ui.components.BadgeKind
import ru.finny.petgame.ui.components.FinnyProgressBar
import ru.finny.petgame.ui.components.PetSprite
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SecondaryButton
import ru.finny.petgame.ui.components.SectionAccent
import ru.finny.petgame.ui.components.SectionHeader
import ru.finny.petgame.ui.model.PetLook
import ru.finny.petgame.ui.model.toPetLook
import ru.finny.petgame.ui.components.SectionTitle
import ru.finny.petgame.ui.components.StatusBadge
import ru.finny.petgame.ui.components.coinsAmount
import ru.finny.petgame.ui.components.weeksAmount
import ru.finny.petgame.ui.theme.FinnyColors

@Composable
fun SavingsScreen(
    repository: GameRepository,
    snapshot: GameSnapshot,
    onBack: () -> Unit,
    onHint: () -> Unit,
    onSavingsChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var goals by remember { mutableStateOf<List<SavingsGoalContent>?>(null) }
    var depositAmount by rememberSaveable { mutableLongStateOf(5L) }
    var withdrawAmount by rememberSaveable { mutableLongStateOf(1L) }
    var withdrawPreview by remember { mutableStateOf<WithdrawResult.NeedsConfirmation?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    var receiveGoalId by remember { mutableStateOf<String?>(null) }
    var achieved by remember { mutableStateOf<GoalAchieveResult.Success?>(null) }
    var eta by remember { mutableStateOf<GoalEta?>(null) }
    var showGoalList by rememberSaveable { mutableStateOf(false) }
    var showWithdraw by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        goals = withContext(Dispatchers.IO) {
            ContentLoader(context.assets).loadCatalog().goals
        }
    }
    val selectedRow = snapshot.savings.firstOrNull { it.goalId == snapshot.selectedGoalId }
    LaunchedEffect(snapshot) {
        eta = selectedRow?.let { repository.getGoalEta(it.goalId) }
    }
    val noActiveGoal = snapshot.selectedGoalId == null

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
                accent = SectionAccent.SAVINGS,
                title = stringResource(R.string.section_savings),
                hint = stringResource(R.string.savings_header_hint),
                petLook = snapshot.profile.toPetLook(),
                petStage = snapshot.profile.petStage,
            )
            Text(
                text = stringResource(R.string.shop_balance_line, snapshot.balance),
                style = MaterialTheme.typography.titleMedium,
            )
            selectedRow?.let { row ->
                val remaining = (row.goalCost - row.savedAmount).coerceAtLeast(0L)
                LaunchedEffect(remaining, row.savedAmount) {
                    if (remaining > 0L && depositAmount > remaining) depositAmount = remaining
                    if (row.savedAmount > 0L && withdrawAmount > row.savedAmount) withdrawAmount = row.savedAmount
                }
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionTitle(
                            text = stringResource(R.string.savings_my_goal),
                            accent = SectionAccent.SAVINGS,
                            icon = Icons.Filled.Star,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            GoalPicture(goalId = row.goalId, modifier = Modifier.size(76.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(text = row.goalTitle, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = stringResource(R.string.savings_saved_line, coinsAmount(row.savedAmount)),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = stringResource(R.string.savings_remaining_line, coinsAmount(remaining)),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                        FinnyProgressBar(
                            progress = if (row.goalCost > 0L) row.savedAmount.toFloat() / row.goalCost else 0f,
                            label = stringResource(R.string.plan_fact_ratio, row.savedAmount, row.goalCost),
                        )
                        if (remaining > 0L) {
                            eta?.periodsLeft?.let { periodsLeft ->
                                Text(
                                    text = stringResource(
                                        R.string.savings_eta,
                                        coinsAmount(eta?.averageDeposit ?: 0L),
                                        weeksAmount(periodsLeft),
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = FinnyColors.TextSecondary,
                                )
                            }
                            AmountStepper(
                                label = stringResource(R.string.savings_amount_label),
                                amount = depositAmount,
                                canDecrease = depositAmount > 1L,
                                canIncrease = depositAmount < remaining,
                                onIncrease = { depositAmount += 1L },
                                onDecrease = { depositAmount -= 1L },
                                onAmountSet = { depositAmount = it },
                                minAmount = 1L,
                                maxAmount = remaining,
                            )
                            PrimaryButton(
                                text = stringResource(R.string.savings_deposit_button),
                                onClick = {
                                    scope.launch {
                                        when (
                                            val result = repository.depositToSavings(row.goalId, depositAmount)
                                        ) {
                                            is DepositResult.Success -> onSavingsChanged()
                                            is DepositResult.InsufficientFunds -> failure = result.explanation
                                            is DepositResult.Invalid -> failure = result.explanation
                                        }
                                    }
                                },
                                containerColor = FinnyColors.Success,
                                edgeColor = FinnyColors.SuccessEdge,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            StatusBadge(
                                text = stringResource(R.string.savings_goal_reached),
                                icon = Icons.Filled.Check,
                                kind = BadgeKind.POSITIVE,
                            )
                            PrimaryButton(
                                text = stringResource(R.string.savings_receive),
                                onClick = { receiveGoalId = row.goalId },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                if (remaining > 0L && row.savedAmount > 0L) {
                    if (!showWithdraw) {
                        SecondaryButton(
                            text = stringResource(R.string.savings_withdraw_open),
                            onClick = { showWithdraw = true },
                        )
                    } else {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                SectionTitle(
                                    text = stringResource(R.string.savings_withdraw_title),
                                    accent = SectionAccent.SAVINGS,
                                    icon = Icons.Filled.Refresh,
                                )
                                AmountStepper(
                                    label = stringResource(R.string.savings_amount_label),
                                    amount = withdrawAmount,
                                    canDecrease = withdrawAmount > 1L,
                                    canIncrease = withdrawAmount < row.savedAmount,
                                    onIncrease = { withdrawAmount += 1L },
                                    onDecrease = { withdrawAmount -= 1L },
                                    onAmountSet = { withdrawAmount = it },
                                    minAmount = 1L,
                                    maxAmount = row.savedAmount,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SecondaryButton(
                                        text = stringResource(R.string.savings_withdraw_button),
                                        onClick = {
                                            scope.launch {
                                                when (
                                                    val result = repository.withdrawFromSavings(
                                                        row.goalId,
                                                        withdrawAmount,
                                                        confirmed = false,
                                                    )
                                                ) {
                                                    is WithdrawResult.NeedsConfirmation -> withdrawPreview = result
                                                    is WithdrawResult.Invalid -> failure = result.explanation
                                                    else -> {}
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                    SecondaryButton(
                                        text = stringResource(R.string.shop_cancel),
                                        onClick = { showWithdraw = false },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            val goalList = goals
            if (goalList == null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (goalList.all { it.id in snapshot.achievedGoalIds }) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    StatusBadge(
                        text = stringResource(R.string.savings_all_received),
                        icon = Icons.Filled.Done,
                        kind = BadgeKind.POSITIVE,
                    )
                }
            } else {
                if (selectedRow != null) {
                    SecondaryButton(
                        text = stringResource(
                            if (showGoalList) R.string.savings_hide_goals else R.string.savings_change_goal,
                        ),
                        onClick = { showGoalList = !showGoalList },
                    )
                }
                if (selectedRow == null || showGoalList) {
                    SectionTitle(
                        text = stringResource(R.string.savings_choose_title),
                        accent = SectionAccent.SAVINGS,
                        icon = Icons.Filled.Star,
                    )
                    if (noActiveGoal && snapshot.achievedGoalIds.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.savings_choose_new),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    goalList
                        .sortedBy { it.id in snapshot.achievedGoalIds }
                        .forEach { goal ->
                            val achieved = goal.id in snapshot.achievedGoalIds
                            GoalCard(
                                goal = goal,
                                selected = goal.id == snapshot.selectedGoalId,
                                achieved = achieved,
                                onClick = {
                                    if (!achieved) {
                                        scope.launch {
                                            repository.selectGoal(goal.id, goal.title, goal.cost)
                                            showGoalList = false
                                            onSavingsChanged()
                                        }
                                    }
                                },
                            )
                        }
                }
            }
        }
    }

    withdrawPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { withdrawPreview = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(text = stringResource(R.string.savings_withdraw_preview_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = stringResource(R.string.savings_preview_saved, coinsAmount(preview.newSavedAmount)))
                    val before = preview.etaBefore?.periodsLeft
                    val after = preview.eta.periodsLeft
                    if (before != null && after != null && after > before) {
                        Text(text = stringResource(R.string.savings_eta_change, weeksAmount(before), weeksAmount(after)))
                    } else if (after != null) {
                        Text(
                            text = stringResource(
                                R.string.savings_eta,
                                coinsAmount(preview.eta.averageDeposit),
                                weeksAmount(after),
                            ),
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.savings_confirm_withdraw),
                        onClick = {
                            withdrawPreview = null
                            scope.launch {
                                selectedRow?.let { row ->
                                    when (
                                        val result = repository.withdrawFromSavings(
                                            row.goalId,
                                            withdrawAmount,
                                            confirmed = true,
                                        )
                                    ) {
                                        is WithdrawResult.Success -> {
                                            showWithdraw = false
                                            onSavingsChanged()
                                        }
                                        is WithdrawResult.Invalid -> failure = result.explanation
                                        else -> {}
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = stringResource(R.string.shop_cancel),
                        onClick = { withdrawPreview = null },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }

    receiveGoalId?.let { goalId ->
        AlertDialog(
            onDismissRequest = { receiveGoalId = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(text = stringResource(R.string.savings_goal_reached)) },
            text = { Text(text = stringResource(R.string.savings_receive_confirm_text)) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.savings_receive),
                        onClick = {
                            receiveGoalId = null
                            scope.launch {
                                when (val result = repository.achieveGoal(goalId)) {
                                    is GoalAchieveResult.Success -> {
                                        achieved = result
                                        onSavingsChanged()
                                    }
                                    is GoalAchieveResult.Invalid -> failure = result.explanation
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = stringResource(R.string.shop_cancel),
                        onClick = { receiveGoalId = null },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }

    achieved?.let { result ->
        AlertDialog(
            onDismissRequest = { achieved = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(text = stringResource(R.string.savings_receive_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = stringResource(R.string.savings_receive_text))
                    Text(text = stringResource(R.string.shop_mood_line, result.moodDelta))
                }
            },
            confirmButton = {
                PrimaryButton(
                    text = stringResource(R.string.hint_close),
                    onClick = { achieved = null },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
    }

    failure?.let { message ->
        AlertDialog(
            onDismissRequest = { failure = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(text = stringResource(R.string.shop_not_enough_title)) },
            text = { Text(text = message) },
            confirmButton = {
                PrimaryButton(
                    text = stringResource(R.string.hint_close),
                    onClick = { failure = null },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
    }
}

@Composable
private fun GoalCard(goal: SavingsGoalContent, selected: Boolean, achieved: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = !achieved,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GoalPicture(goalId = goal.id, modifier = Modifier.size(56.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selected && !achieved) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (achieved) {
                    StatusBadge(
                        text = stringResource(R.string.savings_already_received),
                        icon = Icons.Filled.Done,
                        kind = BadgeKind.POSITIVE,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.savings_goal_cost, coinsAmount(goal.cost)),
                        style = MaterialTheme.typography.titleMedium,
                        color = FinnyColors.Success,
                    )
                }
                Text(text = goal.description, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun GoalPicture(goalId: String, modifier: Modifier = Modifier) {
    val picture = goalPicture(goalId)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(FinnyColors.SoftGreen),
        contentAlignment = Alignment.Center,
    ) {
        when {
            goalId == GameRepository.GOAL_OUTFIT_ID -> FestiveOutfitPreview(Modifier.fillMaxSize())
            picture != null -> Image(
                painter = painterResource(picture),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(6.dp),
            )
            else -> Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = FinnyColors.SplashBarFill,
                modifier = Modifier.fillMaxSize(0.55f),
            )
        }
    }
}

/** Крупно грудь Финни: так наряд виден и в списке мечт, и на карточке копилки. */
@Composable
private fun FestiveOutfitPreview(modifier: Modifier = Modifier) {
    val focusX = 210f
    val focusY = 340f
    val zoom = 2.2f
    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val box = maxWidth
        val sprite = box * zoom
        val scale = sprite / 445f
        val spriteLeft = (sprite - scale * 420f) / 2f
        val dx = box / 2f - (spriteLeft + scale * focusX) + (sprite - box) / 2f
        val dy = box / 2f - scale * focusY + (sprite - box) / 2f
        PetSprite(
            look = PetLook(outfit = PetLook.FESTIVE_OUTFIT, emotion = 1),
            stage = 2,
            modifier = Modifier
                .requiredSize(sprite)
                .offset(x = dx, y = dy),
        )
    }
}
