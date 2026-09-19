package ru.finny.petgame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.PetSprite
import ru.finny.petgame.ui.components.PlayerAvatar

@Composable
fun MainScreen(
    snapshot: GameSnapshot?,
    onHint: () -> Unit,
    onPlan: () -> Unit,
    onSection: (Int) -> Unit,
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
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PetSprite(
                    species = profile.petSpecies,
                    colorIndex = profile.petColor,
                    modifier = Modifier.size(120.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PlayerAvatar(
                        index = profile.playerAvatar,
                        modifier = Modifier.size(48.dp),
                        contentDescription = stringResource(R.string.cd_player_avatar),
                    )
                    Text(
                        text = stringResource(R.string.main_greeting, profile.playerName),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
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
                InfoCard(
                    title = stringResource(R.string.stat_goal_label),
                    body = snapshot.selectedGoalTitle ?: stringResource(R.string.stat_goal_none),
                )
                PetStateCard(mood = profile.mood, saturation = profile.saturation)
                InfoCard(
                    title = stringResource(R.string.active_task_title),
                    body = stringResource(R.string.active_task_stub),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionButton(
                        labelRes = R.string.section_plan,
                        onClick = onPlan,
                        modifier = Modifier.weight(1f),
                    )
                    SectionButton(
                        labelRes = R.string.section_tasks,
                        onClick = { onSection(R.string.section_tasks) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionButton(
                        labelRes = R.string.section_shop,
                        onClick = { onSection(R.string.section_shop) },
                        modifier = Modifier.weight(1f),
                    )
                    SectionButton(
                        labelRes = R.string.section_savings,
                        onClick = { onSection(R.string.section_savings) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionButton(
                        labelRes = R.string.section_progress,
                        onClick = { onSection(R.string.section_progress) },
                        modifier = Modifier.weight(1f),
                    )
                    SectionButton(
                        labelRes = R.string.section_adult,
                        onClick = { onSection(R.string.section_adult) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun PetStateCard(mood: Int, saturation: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = stringResource(R.string.pet_state_title), style = MaterialTheme.typography.titleMedium)
            StatRow(label = stringResource(R.string.pet_state_mood), value = mood)
            StatRow(label = stringResource(R.string.pet_state_saturation), value = saturation)
        }
    }
}

@Composable
private fun StatRow(label: String, value: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = label, modifier = Modifier.weight(1f))
            Text(text = value.toString(), textAlign = TextAlign.End)
        }
        LinearProgressIndicator(
            progress = { value.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SectionButton(
    labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.bodyLarge)
    }
}