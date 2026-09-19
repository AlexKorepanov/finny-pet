package ru.finny.petgame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.OptionCard
import ru.finny.petgame.ui.components.PetSprite

@Composable
fun PetScreen(
    speciesIndex: Int,
    colorIndex: Int,
    petName: String,
    onSpeciesChange: (Int) -> Unit,
    onColorChange: (Int) -> Unit,
    onPetNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onHint: () -> Unit,
    onNext: () -> Unit,
) {
    val speciesLabels = stringArrayResource(R.array.pet_species_labels)
    val colorLabels = stringArrayResource(R.array.pet_color_labels)
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.pet_title),
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PetSprite(species = speciesIndex, colorIndex = colorIndex, modifier = Modifier.size(140.dp))
            Text(
                text = stringResource(R.string.pet_species_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            speciesLabels.forEachIndexed { index, label ->
                OptionCard(
                    selected = index == speciesIndex,
                    onClick = { onSpeciesChange(index) },
                    label = label,
                ) {
                    PetSprite(species = index, colorIndex = colorIndex, modifier = Modifier.size(48.dp))
                }
            }
            Text(
                text = stringResource(R.string.pet_color_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            colorLabels.forEachIndexed { index, label ->
                OptionCard(
                    selected = index == colorIndex,
                    onClick = { onColorChange(index) },
                    label = label,
                ) {
                    PetSprite(species = speciesIndex, colorIndex = index, modifier = Modifier.size(48.dp))
                }
            }
            OutlinedTextField(
                value = petName,
                onValueChange = { value -> if (value.length <= 20) onPetNameChange(value) },
                label = { Text(text = stringResource(R.string.pet_name_label)) },
                placeholder = { Text(text = stringResource(R.string.pet_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onNext,
                enabled = petName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(text = stringResource(R.string.next))
            }
        }
    }
}