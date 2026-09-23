package ru.finny.petgame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.data.model.PeriodCloseResult
import ru.finny.petgame.ui.components.AppCard
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.BadgeKind
import ru.finny.petgame.ui.components.PetSprite
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SectionAccent
import ru.finny.petgame.ui.components.SectionHeader
import ru.finny.petgame.ui.components.StatusBadge
import ru.finny.petgame.ui.components.coinsAmount

@Composable
fun PeriodResultScreen(
    result: PeriodCloseResult.Success,
    petSpecies: Int,
    petColorIndex: Int,
    onContinue: () -> Unit,
    onHint: () -> Unit,
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
                title = stringResource(R.string.period_result_title),
                hint = stringResource(R.string.period_result_hint, result.periodIndex + 1),
                petSpecies = petSpecies,
                petColorIndex = petColorIndex,
                petStage = result.newStage,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PetSprite(
                    species = petSpecies,
                    colorIndex = petColorIndex,
                    stage = result.newStage,
                    modifier = Modifier.size(128.dp),
                )
            }
            if (result.stageGrew) {
                StatusBadge(
                    text = stringResource(R.string.period_stage_grew, stageTitle(result.newStage)),
                    icon = Icons.Filled.Star,
                    kind = BadgeKind.POSITIVE,
                )
            } else {
                StatusBadge(
                    text = stringResource(R.string.period_stage_same, stageTitle(result.newStage)),
                    icon = Icons.Filled.Check,
                    kind = BadgeKind.NEUTRAL,
                )
            }
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.period_checks_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    CriterionRow(
                        ok = result.review.requiredCovered,
                        text = stringResource(R.string.period_check_required),
                    )
                    CriterionRow(
                        ok = result.review.planMatched,
                        text = stringResource(R.string.period_check_plan),
                    )
                    CriterionRow(
                        ok = result.review.savingsRegular,
                        text = stringResource(R.string.period_check_savings),
                    )
                }
            }
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.period_mood_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            R.string.period_mood_change,
                            result.previousMood,
                            result.newMood,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    result.review.explanations.forEach { line ->
                        Text(text = line, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (result.periodIncome > 0L) {
                        stringResource(
                            R.string.period_income_line,
                            coinsAmount(result.periodIncome),
                            coinsAmount(result.newBalance),
                        )
                    } else {
                        stringResource(
                            R.string.period_income_none_line,
                            coinsAmount(result.newBalance),
                        )
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            PrimaryButton(
                text = stringResource(R.string.period_continue),
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CriterionRow(ok: Boolean, text: String) {
    StatusBadge(
        text = text,
        icon = if (ok) Icons.Filled.Check else Icons.Filled.Warning,
        kind = if (ok) BadgeKind.POSITIVE else BadgeKind.ATTENTION,
    )
}

@Composable
fun stageTitle(stage: Int): String = when (stage.coerceIn(0, 2)) {
    0 -> stringResource(R.string.pet_stage_0)
    1 -> stringResource(R.string.pet_stage_1)
    else -> stringResource(R.string.pet_stage_2)
}
