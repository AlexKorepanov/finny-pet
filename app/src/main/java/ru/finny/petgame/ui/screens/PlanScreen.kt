package ru.finny.petgame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
import ru.finny.petgame.ui.components.BadgeKind
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SectionAccent
import ru.finny.petgame.ui.components.SectionHeader
import ru.finny.petgame.ui.components.StatusBadge

@Composable
fun PlanScreen(
    repository: GameRepository,
    snapshot: GameSnapshot,
    onBack: () -> Unit,
    onHint: () -> Unit,
    onPlanConfirmed: () -> Unit,
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
                petSpecies = snapshot.profile.petSpecies,
                petColorIndex = snapshot.profile.petColor,
            )
            if (editable) {
                Text(
                    text = stringResource(R.string.plan_available, available),
                    style = MaterialTheme.typography.titleMedium,
                )
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    DirectionRow(
                        label = stringResource(R.string.plan_direction_required),
                        amount = requiredAmount,
                        canIncrease = total < available,
                        canDecrease = requiredAmount > 0L,
                        onIncrease = { requiredAmount += 1L },
                        onDecrease = { requiredAmount -= 1L },
                    )
                    DirectionRow(
                        label = stringResource(R.string.plan_direction_optional),
                        amount = optionalAmount,
                        canIncrease = total < available,
                        canDecrease = optionalAmount > 0L,
                        onIncrease = { optionalAmount += 1L },
                        onDecrease = { optionalAmount -= 1L },
                    )
                    DirectionRow(
                        label = stringResource(R.string.plan_direction_savings),
                        amount = savingsAmount,
                        canIncrease = total < available,
                        canDecrease = savingsAmount > 0L,
                        onIncrease = { savingsAmount += 1L },
                        onDecrease = { savingsAmount -= 1L },
                    )
                    Text(
                        text = stringResource(R.string.plan_remainder, remainder),
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
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.plan_label),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(56.dp),
                            textAlign = TextAlign.End,
                        )
                        Text(
                            text = stringResource(R.string.fact_label),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(56.dp),
                            textAlign = TextAlign.End,
                        )
                    }
                    listOf(
                        BudgetDirection.REQUIRED,
                        BudgetDirection.OPTIONAL,
                        BudgetDirection.SAVINGS,
                    ).forEach { direction ->
                        PlanFactRow(
                            label = directionLabel(direction),
                            planAmount = planByDirection[direction] ?: 0L,
                            factAmount = snapshot.periodFact[direction] ?: 0L,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectionRow(
    label: String,
    amount: Long,
    canIncrease: Boolean,
    canDecrease: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
) {
    val increaseDesc = stringResource(R.string.cd_increase)
    val decreaseDesc = stringResource(R.string.cd_decrease)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(
            onClick = onDecrease,
            enabled = canDecrease,
            modifier = Modifier.semantics { contentDescription = decreaseDesc },
        ) {
            Text(
                text = stringResource(R.string.plan_decrease),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        Text(
            text = amount.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End,
        )
        IconButton(
            onClick = onIncrease,
            enabled = canIncrease,
            modifier = Modifier.semantics { contentDescription = increaseDesc },
        ) {
            Text(
                text = stringResource(R.string.plan_increase),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@Composable
private fun PlanFactRow(label: String, planAmount: Long, factAmount: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = planAmount.toString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(56.dp),
            textAlign = TextAlign.End,
        )
        Text(
            text = factAmount.toString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(56.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun directionLabel(direction: BudgetDirection): String = when (direction) {
    BudgetDirection.REQUIRED -> stringResource(R.string.plan_direction_required)
    BudgetDirection.OPTIONAL -> stringResource(R.string.plan_direction_optional)
    BudgetDirection.SAVINGS -> stringResource(R.string.plan_direction_savings)
}