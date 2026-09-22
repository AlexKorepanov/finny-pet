package ru.finny.petgame.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R

@Composable
fun AmountStepper(
    label: String,
    amount: Long,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    canIncrease: Boolean = true,
    canDecrease: Boolean = true,
) {
    val increaseDesc = stringResource(R.string.cd_increase)
    val decreaseDesc = stringResource(R.string.cd_decrease)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            caption?.let { captionText ->
                Text(
                    text = captionText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(
            onClick = onDecrease,
            enabled = canDecrease,
            modifier = Modifier.semantics { contentDescription = decreaseDesc },
        ) {
            Text(
                text = stringResource(R.string.plan_decrease),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        Text(
            text = amount.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End,
        )
        IconButton(
            onClick = onIncrease,
            enabled = canIncrease,
            modifier = Modifier.semantics { contentDescription = increaseDesc },
        ) {
            Text(
                text = stringResource(R.string.plan_increase),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}