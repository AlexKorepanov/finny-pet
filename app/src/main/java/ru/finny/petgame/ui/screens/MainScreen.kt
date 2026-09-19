package ru.finny.petgame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.ui.components.AppCard
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.FinnyProgressBar
import ru.finny.petgame.ui.components.PetSprite
import ru.finny.petgame.ui.components.PlayerAvatar
import ru.finny.petgame.ui.components.SectionAccent
import ru.finny.petgame.ui.components.sectionAccentColor
import ru.finny.petgame.ui.theme.FinnyColors

@Composable
fun MainScreen(
    snapshot: GameSnapshot?,
    onHint: () -> Unit,
    onPlan: () -> Unit,
    onShop: () -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PetSprite(
                            species = profile.petSpecies,
                            colorIndex = profile.petColor,
                            modifier = Modifier.size(96.dp),
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
                    }
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
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.stat_goal_label), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = snapshot.selectedGoalTitle ?: stringResource(R.string.stat_goal_none),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.pet_state_title), style = MaterialTheme.typography.titleMedium)
                    MoodRow(label = stringResource(R.string.pet_state_mood), value = profile.mood)
                    MoodRow(label = stringResource(R.string.pet_state_saturation), value = profile.saturation)
                }
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.active_task_title), style = MaterialTheme.typography.titleMedium)
                    Text(text = stringResource(R.string.active_task_stub), style = MaterialTheme.typography.bodyLarge)
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
                        onClick = { onSection(R.string.section_tasks) },
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
                        onClick = { onSection(R.string.section_savings) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTile(
                        label = stringResource(R.string.section_progress),
                        icon = Icons.Filled.Star,
                        accent = SectionAccent.PROGRESS,
                        onClick = { onSection(R.string.section_progress) },
                        modifier = Modifier.weight(1f),
                    )
                    SectionTile(
                        label = stringResource(R.string.section_adult),
                        icon = Icons.Filled.Lock,
                        accent = SectionAccent.ADULT,
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
    AppCard(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun MoodRow(label: String, value: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
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
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 96.dp),
        shape = MaterialTheme.shapes.large,
        color = sectionAccentColor(accent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FinnyColors.OnPrimary,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = label,
                color = FinnyColors.OnPrimary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}