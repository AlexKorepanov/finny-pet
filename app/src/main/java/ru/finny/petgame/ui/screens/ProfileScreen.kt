package ru.finny.petgame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import ru.finny.petgame.ui.components.PetSprite

@Composable
fun ProfileScreen(
    playerName: String,
    speciesIndex: Int,
    onPlayerNameChange: (String) -> Unit,
    onSpeciesChange: (Int) -> Unit,
    onBack: () -> Unit,
    onHint: () -> Unit,
    onNext: () -> Unit,
) {
    val speciesLabels = stringArrayResource(R.array.pet_species_labels)
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
            Text(
                text = stringResource(R.string.character_title),
                style = MaterialTheme.typography.titleMedium,
            )
            speciesLabels.forEachIndexed { index, label ->
                OptionCard(
                    selected = index == speciesIndex,
                    onClick = { onSpeciesChange(index) },
                    label = label,
                ) {
                    PetSprite(species = index, colorIndex = 0, modifier = Modifier.size(48.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onNext,
                enabled = playerName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(text = stringResource(R.string.next))
            }
        }
    }
}