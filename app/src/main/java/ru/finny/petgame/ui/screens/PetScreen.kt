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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.ChunkySurface
import ru.finny.petgame.ui.components.OptionCard
import ru.finny.petgame.ui.components.PetSprite
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SectionTitle
import ru.finny.petgame.ui.theme.FinnyColors

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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ChunkySurface(
                onClick = null,
                modifier = Modifier.fillMaxWidth(),
                containerColor = FinnyColors.SoftBlue,
                edgeColor = FinnyColors.PrimaryEdge.copy(alpha = 0.35f),
                borderColor = FinnyColors.Primary.copy(alpha = 0.3f),
                minHeight = 180.dp,
            ) {
                PetSprite(
                    species = speciesIndex,
                    colorIndex = colorIndex,
                    modifier = Modifier.size(160.dp),
                )
            }
            SectionTitle(
                text = stringResource(R.string.pet_species_title),
                icon = Icons.Filled.Person,
                modifier = Modifier.fillMaxWidth(),
            )
            speciesLabels.forEachIndexed { index, label ->
                OptionCard(
                    selected = index == speciesIndex,
                    onClick = { onSpeciesChange(index) },
                    label = label,
                ) {
                    PetSprite(species = index, colorIndex = colorIndex, modifier = Modifier.size(56.dp))
                }
            }
            SectionTitle(
                text = stringResource(R.string.pet_color_title),
                modifier = Modifier.fillMaxWidth(),
            )
            colorLabels.forEachIndexed { index, label ->
                OptionCard(
                    selected = index == colorIndex,
                    onClick = { onColorChange(index) },
                    label = label,
                ) {
                    PetSprite(species = speciesIndex, colorIndex = index, modifier = Modifier.size(56.dp))
                }
            }
            OutlinedTextField(
                value = petName,
                onValueChange = { value -> if (value.length <= 20) onPetNameChange(value) },
                label = { Text(text = stringResource(R.string.pet_name_label)) },
                placeholder = { Text(text = stringResource(R.string.pet_name_hint)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                text = stringResource(R.string.next),
                onClick = onNext,
                enabled = petName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
