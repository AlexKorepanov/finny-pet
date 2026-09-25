package ru.finny.petgame.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.economy.EconomyEngine
import ru.finny.petgame.ui.screens.stageTitle
import ru.finny.petgame.ui.theme.FinnyColors

/** Что такое звёздочки роста, сколько их и как получить ещё. */
@Composable
fun StarsDialog(stars: Int, stage: Int, onClose: () -> Unit) {
    val left = EconomyEngine().starsToNextStage(stars)
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        icon = {
            Icon(Icons.Filled.Star, null, tint = FinnyColors.SplashBarFill, modifier = Modifier.size(40.dp))
        },
        title = { Text(text = stringResource(R.string.stars_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.stars_dialog_have, starsAmount(stars), stageTitle(stage)),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (left != null) {
                    val to = if (stage == 0) EconomyEngine.STARS_FOR_TEEN else EconomyEngine.STARS_FOR_ADULT
                    FinnyProgressBar(
                        progress = stars.toFloat() / to,
                        label = stringResource(R.string.stars_dialog_progress, stars, to),
                    )
                    Text(
                        text = stringResource(R.string.stars_dialog_left, stageTitle(stage + 1), starsAmount(left)),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.stars_dialog_max),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(
                    text = stringResource(R.string.stars_dialog_how),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
                StarRule(R.string.stars_rule_needs)
                StarRule(R.string.stars_rule_plan)
                StarRule(R.string.stars_rule_savings)
                Text(
                    text = stringResource(R.string.stars_dialog_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinnyColors.TextSecondary,
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.hint_close),
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

@Composable
private fun StarRule(text: Int) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            Icons.Filled.Star,
            null,
            tint = FinnyColors.SplashBarFill,
            modifier = Modifier.size(20.dp).padding(top = 2.dp),
        )
        Text(text = stringResource(text), style = MaterialTheme.typography.bodyLarge)
    }
}
