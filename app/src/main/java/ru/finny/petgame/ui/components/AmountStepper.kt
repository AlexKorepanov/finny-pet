package ru.finny.petgame.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import ru.finny.petgame.ui.theme.FinnyColors

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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            caption?.let { captionText ->
                Text(
                    text = captionText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        StepperButton(
            text = stringResource(R.string.plan_decrease),
            enabled = canDecrease,
            onClick = onDecrease,
            contentDescription = decreaseDesc,
        )
        Text(
            text = amount.toString(),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.widthIn(min = 44.dp),
            textAlign = TextAlign.Center,
        )
        StepperButton(
            text = stringResource(R.string.plan_increase),
            enabled = canIncrease,
            onClick = onIncrease,
            contentDescription = increaseDesc,
        )
    }
}

@Composable
private fun StepperButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
) {
    ChunkySurface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(52.dp)
            .semantics { this.contentDescription = contentDescription },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        edgeColor = FinnyColors.PrimaryEdge,
        borderColor = MaterialTheme.colorScheme.primary,
        borderWidth = 2.dp,
        minHeight = 52.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
