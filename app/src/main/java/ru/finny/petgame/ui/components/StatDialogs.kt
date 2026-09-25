package ru.finny.petgame.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
import ru.finny.petgame.data.entity.SavingsEntity
import ru.finny.petgame.economy.EconomyEngine
import ru.finny.petgame.ui.theme.FinnyColors

/** Общий вид окон «что это за значок» на главном экране. */
@Composable
internal fun StatInfoDialog(
    icon: @Composable () -> Unit,
    title: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        icon = icon,
        title = { Text(text = title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
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
internal fun StatLead(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}

@Composable
internal fun StatSection(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
internal fun StatNote(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = FinnyColors.TextSecondary)
}

@Composable
internal fun StatRule(text: String, icon: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.padding(top = 2.dp)) { icon() }
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Сколько монет, откуда они берутся и на что идут. */
@Composable
fun CoinsDialog(balance: Long, onClose: () -> Unit) {
    val coin: @Composable () -> Unit = { CoinIcon(Modifier.size(20.dp)) }
    StatInfoDialog(
        icon = { CoinIcon(Modifier.size(40.dp)) },
        title = stringResource(R.string.coins_dialog_title),
        onClose = onClose,
    ) {
        StatLead(stringResource(R.string.coins_dialog_have, coinsAmount(balance)))
        StatSection(stringResource(R.string.coins_dialog_how))
        StatRule(stringResource(R.string.coins_rule_pocket, coinsAmount(EconomyEngine.PERIOD_INCOME_AMOUNT)), coin)
        StatRule(stringResource(R.string.coins_rule_tasks), coin)
        StatRule(stringResource(R.string.coins_rule_plan, coinsAmount(EconomyEngine.PLAN_BONUS_AMOUNT)), coin)
        StatSection(stringResource(R.string.coins_dialog_where))
        StatRule(stringResource(R.string.coins_use_needs), coin)
        StatRule(stringResource(R.string.coins_use_wishes), coin)
        StatRule(stringResource(R.string.coins_use_savings), coin)
        StatNote(stringResource(R.string.coins_dialog_note))
    }
}

/** Сколько в копилке, как идёт текущая мечта и как работает копилка. */
@Composable
fun SavingsDialog(total: Long, goal: SavingsEntity?, onClose: () -> Unit) {
    val heart: @Composable () -> Unit = {
        Icon(Icons.Filled.Favorite, null, tint = FinnyColors.Success, modifier = Modifier.size(20.dp))
    }
    StatInfoDialog(
        icon = { Icon(Icons.Filled.Favorite, null, tint = FinnyColors.Success, modifier = Modifier.size(40.dp)) },
        title = stringResource(R.string.savings_dialog_title),
        onClose = onClose,
    ) {
        StatLead(stringResource(R.string.savings_dialog_have, coinsAmount(total)))
        if (goal != null && goal.goalCost > 0) {
            val saved = goal.savedAmount.coerceAtMost(goal.goalCost)
            Text(
                text = stringResource(R.string.savings_dialog_goal, goal.goalTitle),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            FinnyProgressBar(
                progress = saved.toFloat() / goal.goalCost,
                label = stringResource(R.string.stars_dialog_progress, saved.toInt(), goal.goalCost.toInt()),
            )
            Text(
                text = stringResource(R.string.savings_dialog_left, coinsAmount(goal.goalCost - saved)),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            Text(text = stringResource(R.string.savings_dialog_no_goal), style = MaterialTheme.typography.bodyLarge)
        }
        StatSection(stringResource(R.string.savings_dialog_how))
        StatRule(stringResource(R.string.savings_rule_put), heart)
        StatRule(stringResource(R.string.savings_rule_reach), heart)
        StatRule(stringResource(R.string.savings_rule_take), heart)
        StatNote(stringResource(R.string.savings_dialog_note))
    }
}
