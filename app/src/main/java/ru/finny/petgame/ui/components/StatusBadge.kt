package ru.finny.petgame.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.finny.petgame.ui.theme.FinnyColors

enum class BadgeKind {
    POSITIVE,
    ATTENTION,
    NEUTRAL,
}

@Composable
fun StatusBadge(
    text: String,
    icon: ImageVector,
    kind: BadgeKind,
    modifier: Modifier = Modifier,
) {
    val colors: Pair<Color, Color> = when (kind) {
        BadgeKind.POSITIVE -> FinnyColors.BadgePositiveContainer to FinnyColors.BadgePositiveContent
        BadgeKind.ATTENTION -> FinnyColors.BadgeAttentionContainer to FinnyColors.BadgeAttentionContent
        BadgeKind.NEUTRAL -> FinnyColors.BadgeNeutralContainer to FinnyColors.BadgeNeutralContent
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = colors.first,
        border = BorderStroke(1.dp, colors.second.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.second,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = text,
                color = colors.second,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}