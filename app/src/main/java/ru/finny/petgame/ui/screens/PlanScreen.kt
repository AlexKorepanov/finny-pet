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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.finny.petgame.R
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.data.model.PeriodStatus
import ru.finny.petgame.data.repository.GameRepository
import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.economy.model.BudgetPlan
import ru.finny.petgame.economy.model.PlanCheckResult
import ru.finny.petgame.ui.components.AppCard
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.AmountStepper
import ru.finny.petgame.ui.components.BadgeKind
import ru.finny.petgame.ui.components.FinnyProgressBar
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SectionAccent
import ru.finny.petgame.ui.components.SectionHeader
import ru.finny.petgame.ui.model.toPetLook
import ru.finny.petgame.ui.components.StatusBadge
import ru.finny.petgame.ui.components.coinsAmount

@Composable
fun PlanScreen(
    repository: GameRepository,
    snapshot: GameSnapshot,
    onBack: () -> Unit,
    onHint: () -> Unit,
    onPlanConfirmed: () -> Unit,
    onClosePeriod: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val status = snapshot.currentPeriod?.status
    val editable = status == null || status == PeriodStatus.PLANNED.name
    val savedPlan = snapshot.plan.associate { BudgetDirection.valueOf(it.direction) to it.plannedAmount }
    var requiredAmount by rememberSaveable { mutableLongStateOf(savedPlan[BudgetDirection.REQUIRED] ?: 0L) }
    var optionalAmount by rememberSaveable { mutableLongStateOf(savedPlan[BudgetDirection.OPTIONAL] ?: 0L) }
    var savingsAmount by rememberSaveable { mutableLongStateOf(savedPlan[BudgetDirection.SAVINGS] ?: 0L) }
    var problems by remember { mutableStateOf<List<String>>(emptyList()) }

    val total = requiredAmount + optionalAmount + savingsAmount
    val available = snapshot.balance
    val remainder = available - total

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
                accent = SectionAccent.PLAN,
                title = stringResource(R.string.section_plan),
                hint = stringResource(R.string.plan_edit_hint),
                petLook = snapshot.profile.toPetLook(),
                petStage = snapshot.profile.petStage,
            )
            if (editable) {
                Text(
                    text = stringResource(R.string.plan_available, coinsAmount(available)),
                    style = MaterialTheme.typography.titleMedium,
                )
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    AmountStepper(
                        label = stringResource(R.string.plan_direction_required),
                        caption = stringResource(R.string.plan_hint_required),
                        amount = requiredAmount,
                        canIncrease = total < available,
                        canDecrease = requiredAmount > 0L,
                        onIncrease = { requiredAmount += 1L },
                        onDecrease = { requiredAmount -= 1L },
                    )
                    AmountStepper(
                        label = stringResource(R.string.plan_direction_optional),
                        caption = stringResource(R.string.plan_hint_optional),
                        amount = optionalAmount,
                        canIncrease = total < available,
                        canDecrease = optionalAmount > 0L,
                        onIncrease = { optionalAmount += 1L },
                        onDecrease = { optionalAmount -= 1L },
                    )
                    AmountStepper(
                        label = stringResource(R.string.plan_direction_savings),
                        caption = stringResource(R.string.plan_hint_savings),
                        amount = savingsAmount,
                        canIncrease = total < available,
                        canDecrease = savingsAmount > 0L,
                        onIncrease = { savingsAmount += 1L },
                        onDecrease = { savingsAmount -= 1L },
                    )
                    Text(
                        text = stringResource(R.string.plan_remainder, coinsAmount(remainder)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                problems.forEach { problem ->
                    StatusBadge(
                        text = problem,
                        icon = Icons.Filled.Warning,
                        kind = BadgeKind.ATTENTION,
                    )
                }
                PrimaryButton(
                    text = stringResource(R.string.plan_confirm),
                    onClick = {
                        scope.launch {
                            val check = repository.saveBudgetPlan(
                                BudgetPlan(
                                    mapOf(
                                        BudgetDirection.REQUIRED to requiredAmount,
                                        BudgetDirection.OPTIONAL to optionalAmount,
                                        BudgetDirection.SAVINGS to savingsAmount,
                                    ),
                                ),
                            )
                            when (check) {
                                is PlanCheckResult.Valid -> {
                                    repository.confirmBudgetPlan()
                                    onPlanConfirmed()
                                }
                                is PlanCheckResult.Invalid -> problems = check.problems
                            }
                        }
                    },
                    enabled = total > 0L,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                StatusBadge(
                    text = stringResource(R.string.plan_confirmed_badge),
                    icon = Icons.Filled.Check,
                    kind = BadgeKind.POSITIVE,
                )
                val planByDirection = snapshot.plan.associate {
                    BudgetDirection.valueOf(it.direction) to it.plannedAmount
                }
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        BudgetDirection.REQUIRED,
                        BudgetDirection.OPTIONAL,
                        BudgetDirection.SAVINGS,
                    ).forEach { direction ->
                        PlanFactRow(
                            label = directionLabel(direction),
                            caption = directionCaption(direction),
                            planAmount = planByDirection[direction] ?: 0L,
                            factAmount = snapshot.periodFact[direction] ?: 0L,
                        )
                    }
                }
                if (status == PeriodStatus.ACTIVE.name) {
                    PrimaryButton(
                        text = stringResource(R.string.period_close_button),
                        onClick = onClosePeriod,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanFactRow(label: String, caption: String, planAmount: Long, factAmount: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = caption,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FinnyProgressBar(
            progress = if (planAmount > 0L) factAmount.toFloat() / planAmount else 0f,
            label = stringResource(R.string.plan_fact_ratio, factAmount, planAmount),
        )
        if (factAmount > planAmount) {
            Text(text = stringResource(R.string.plan_over_note), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun directionLabel(direction: BudgetDirection): String = when (direction) {
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