package ru.finny.petgame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.ChunkySurface
import ru.finny.petgame.ui.components.PetSprite
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SecondaryButton
import ru.finny.petgame.ui.model.PetLook
import ru.finny.petgame.ui.theme.FinnyColors

@Composable
fun PetConfirmScreen(
    petName: String,
    look: PetLook,
    onBack: () -> Unit,
    onHint: () -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.confirm_title),
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ChunkySurface(
                onClick = null,
                modifier = Modifier.fillMaxWidth(),
                containerColor = FinnyColors.SoftGreen,
                edgeColor = FinnyColors.SuccessEdge.copy(alpha = 0.4f),
                borderColor = FinnyColors.Success.copy(alpha = 0.35f),
                minHeight = 220.dp,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PetSprite(
                        look = look,
                        modifier = Modifier.size(168.dp),
                    )
                    Text(
                        text = petName,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecondaryButton(
                    text = stringResource(R.string.edit),
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = stringResource(R.string.save),
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
