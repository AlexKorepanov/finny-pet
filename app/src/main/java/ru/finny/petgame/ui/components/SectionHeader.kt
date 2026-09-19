package ru.finny.petgame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.finny.petgame.ui.theme.FinnyColors

enum class SectionAccent {
    PLAN,
    SHOP,
    TASKS,
    SAVINGS,
    PROGRESS,
    ADULT,
}

fun sectionAccentColor(accent: SectionAccent): Color = when (accent) {
    SectionAccent.PLAN -> FinnyColors.Primary
    SectionAccent.SHOP -> FinnyColors.Optional
    SectionAccent.TASKS -> FinnyColors.Tasks
    SectionAccent.SAVINGS -> FinnyColors.Success
    SectionAccent.PROGRESS -> FinnyColors.Teal
    SectionAccent.ADULT -> FinnyColors.Adult
}

@Composable
fun SectionHeader(
    accent: SectionAccent,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    petSpecies: Int = 0,
    petColorIndex: Int = 0,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(sectionAccentColor(accent))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = FinnyColors.OnPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = hint,
                color = FinnyColors.OnPrimary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        PetSprite(
            species = petSpecies,
            colorIndex = petColorIndex,
            modifier = Modifier.size(56.dp),
        )
    }
}