package ru.finny.petgame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.finny.petgame.ui.theme.FinnyColors

/**
 * «Толстый» блок в духе обучающих приложений: нижняя кромка даёт объём без теней.
 */
@Composable
fun ChunkySurface(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    edgeColor: Color = FinnyColors.CardBorder,
    borderColor: Color = if (selected) MaterialTheme.colorScheme.primary else FinnyColors.CardBorder,
    borderWidth: Dp = if (selected) 3.dp else 2.dp,
    minHeight: Dp = 56.dp,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = MaterialTheme.shapes.large
    val edge = if (enabled) edgeColor else edgeColor.copy(alpha = 0.45f)
    val face = if (enabled) containerColor else containerColor.copy(alpha = 0.55f)
    val pressable = onClick != null && enabled

    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .clip(shape)
            .background(edge)
            .padding(bottom = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = if (pressed && pressable) 4.dp else 0.dp)
                .clip(shape)
                .background(face)
                .border(borderWidth, if (enabled) borderColor else borderColor.copy(alpha = 0.4f), shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interaction,
                            indication = null,
                            enabled = enabled,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}

