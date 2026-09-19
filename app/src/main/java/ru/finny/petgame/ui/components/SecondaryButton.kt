package ru.finny.petgame.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}