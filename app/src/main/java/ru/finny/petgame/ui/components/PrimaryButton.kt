package ru.finny.petgame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.finny.petgame.ui.theme.FinnyColors

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = MaterialTheme.shapes.medium
    val faceColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val edgeColor = if (enabled) FinnyColors.PrimaryEdge else FinnyColors.PrimaryEdge.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(edgeColor)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .offset(y = if (pressed && enabled) 4.dp else 0.dp)
                .background(faceColor)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}