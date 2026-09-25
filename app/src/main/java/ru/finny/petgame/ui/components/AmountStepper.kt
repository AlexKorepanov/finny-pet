package ru.finny.petgame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.ui.theme.FinnyColors

private const val MAX_TYPED_DIGITS = 5
private val AmountShape = RoundedCornerShape(14.dp)

/**
 * Счётчик монет с кнопками «−» и «+».
 * Если задан [onAmountSet], на число можно нажать и ввести его с клавиатуры
 * в пределах [minAmount]..[maxAmount].
 */
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
    onAmountSet: ((Long) -> Unit)? = null,
    minAmount: Long = 0L,
    maxAmount: Long? = null,
) {
    val increaseDesc = stringResource(R.string.cd_increase)
    val decreaseDesc = stringResource(R.string.cd_decrease)
    var typing by rememberSaveable { mutableStateOf(false) }
    if (typing && onAmountSet != null) {
        AmountInputDialog(
            title = label,
            initial = amount,
            minAmount = minAmount,
            maxAmount = maxAmount?.coerceAtLeast(minAmount),
            onDone = {
                onAmountSet(it)
                typing = false
            },
            onCancel = { typing = false },
        )
    }
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
        if (onAmountSet != null) {
            val typeDesc = stringResource(R.string.cd_amount_type, amount)
            Box(
                modifier = Modifier
                    .widthIn(min = 60.dp)
                    .height(52.dp)
                    .clip(AmountShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, MaterialTheme.colorScheme.outline, AmountShape)
                    .clickable(role = Role.Button, onClick = { typing = true })
                    .semantics { contentDescription = typeDesc }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = amount.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Text(
                text = amount.toString(),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.widthIn(min = 44.dp),
                textAlign = TextAlign.Center,
            )
        }
        StepperButton(
            text = stringResource(R.string.plan_increase),
            enabled = canIncrease,
            onClick = onIncrease,
            contentDescription = increaseDesc,
        )
    }
}

@Composable
private fun AmountInputDialog(
    title: String,
    initial: Long,
    minAmount: Long,
    maxAmount: Long?,
    onDone: (Long) -> Unit,
    onCancel: () -> Unit,
) {
    val start = initial.toString()
    var field by remember { mutableStateOf(TextFieldValue(start, TextRange(0, start.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val typed = field.text.toLongOrNull()
    val valid = typed != null && typed >= minAmount && (maxAmount == null || typed <= maxAmount)
    val range = if (maxAmount != null) {
        stringResource(R.string.amount_input_range, minAmount, maxAmount)
    } else {
        stringResource(R.string.amount_input_min, minAmount)
    }
    val confirm = { if (valid) onDone(typed!!) }
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = field,
                    onValueChange = { new ->
                        val digits = new.text.filter(Char::isDigit).take(MAX_TYPED_DIGITS)
                        field = if (digits == new.text) new else TextFieldValue(digits, TextRange(digits.length))
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    isError = field.text.isNotEmpty() && !valid,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                Text(
                    text = if (field.text.isNotEmpty() && !valid) {
                        stringResource(R.string.amount_input_out_of_range, range)
                    } else {
                        range
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (field.text.isNotEmpty() && !valid) FinnyColors.BadgeAttentionContent else FinnyColors.TextSecondary,
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.amount_input_done),
                onClick = confirm,
                enabled = valid,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        dismissButton = {
            SecondaryButton(text = stringResource(R.string.amount_input_cancel), onClick = onCancel)
        },
    )
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
