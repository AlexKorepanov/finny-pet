package ru.finny.petgame.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.economy.EconomyEngine
import ru.finny.petgame.ui.screens.stageTitle
import ru.finny.petgame.ui.theme.FinnyColors

/** Что такое звёздочки роста, сколько их и как получить ещё. */
@Composable
fun StarsDialog(stars: Int, stage: Int, onClose: () -> Unit) {
    val left = EconomyEngine().starsToNextStage(stars)
    val star: @Composable () -> Unit = {
        Icon(Icons.Filled.Star, null, tint = FinnyColors.SplashBarFill, modifier = Modifier.size(20.dp))
    }
    StatInfoDialog(
        icon = { Icon(Icons.Filled.Star, null, tint = FinnyColors.SplashBarFill, modifier = Modifier.size(40.dp)) },
        title = stringResource(R.string.stars_dialog_title),
        onClose = onClose,
    ) {
        StatLead(stringResource(R.string.stars_dialog_have, starsAmount(stars), stageTitle(stage)))
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
            Text(text = stringResource(R.string.stars_dialog_max), style = MaterialTheme.typography.bodyLarge)
        }
        StatSection(stringResource(R.string.stars_dialog_how))
        StatRule(stringResource(R.string.stars_rule_needs), star)
        StatRule(stringResource(R.string.stars_rule_plan), star)
        StatRule(stringResource(R.string.stars_rule_savings), star)
        StatNote(stringResource(R.string.stars_dialog_note))
    }
}
