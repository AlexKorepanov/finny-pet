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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.OptionCard
import ru.finny.petgame.ui.components.PlayerAvatar
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SectionTitle

@Composable
fun ProfileScreen(
    playerName: String,
    avatarIndex: Int,
    onPlayerNameChange: (String) -> Unit,
    onAvatarChange: (Int) -> Unit,
    onBack: () -> Unit,
    onHint: () -> Unit,
    onNext: () -> Unit,
) {
    val avatarLabels = stringArrayResource(R.array.player_avatar_labels)
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.profile_title),
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
            OutlinedTextField(
                value = playerName,
                onValueChange = { value -> if (value.length <= 20) onPlayerNameChange(value) },
                label = { Text(text = stringResource(R.string.player_name_label)) },
                placeholder = { Text(text = stringResource(R.string.player_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.profile_guest_note),
                style = MaterialTheme.typography.bodyLarge,
            )
            SectionTitle(
                text = stringResource(R.string.character_title),
                icon = Icons.Filled.Person,
            )
            avatarLabels.forEachIndexed { index, label ->
                OptionCard(
                    selected = index == avatarIndex,
                    onClick = { onAvatarChange(index) },
                    label = label,
                ) {
                    PlayerAvatar(index = index, modifier = Modifier.size(48.dp))
                }
            }
            PrimaryButton(
                text = stringResource(R.string.next),
                onClick = onNext,
                enabled = playerName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}