package ru.finny.petgame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import ru.finny.petgame.ui.theme.FinnyColors
import ru.finny.petgame.ui.model.PetLook

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

/** Цвет раздела для текста и значков: в тёмной теме светлее, иначе сливается с подкрашенным фоном. */
fun sectionAccentText(accent: SectionAccent): Color = if (FinnyColors.isDark()) {
    when (accent) {
        SectionAccent.PLAN -> Color(0xFF9DBBF7)
        SectionAccent.SHOP -> Color(0xFFF2B26E)
        SectionAccent.TASKS -> Color(0xFFC9B0F4)
        SectionAccent.SAVINGS -> Color(0xFF8DD9A2)
        SectionAccent.PROGRESS -> Color(0xFF7ED8CA)
        SectionAccent.ADULT -> Color(0xFFC0CAD3)
    }
} else {
    sectionAccentColor(accent)
}

fun sectionAccentEdge(accent: SectionAccent): Color = when (accent) {
    SectionAccent.PLAN -> FinnyColors.PrimaryEdge
    SectionAccent.SHOP -> FinnyColors.OptionalEdge
    SectionAccent.TASKS -> FinnyColors.TasksEdge
    SectionAccent.SAVINGS -> FinnyColors.SuccessEdge
    SectionAccent.PROGRESS -> FinnyColors.TealEdge
    SectionAccent.ADULT -> FinnyColors.AdultEdge
}

/** Фон всего экрана раздела: мягкий оттенок цвета иконки в нижнем меню. */
fun sectionBackground(accent: SectionAccent): Color = if (FinnyColors.isDark()) {
    when (accent) {
        SectionAccent.PLAN -> Color(0xFF1B2333)
        SectionAccent.SHOP -> Color(0xFF2E2216)
        SectionAccent.TASKS -> Color(0xFF251D35)
        SectionAccent.SAVINGS -> Color(0xFF182A1D)
        SectionAccent.PROGRESS -> Color(0xFF152A27)
        SectionAccent.ADULT -> Color(0xFF22262A)
    }
} else {
    when (accent) {
        SectionAccent.PLAN -> Color(0xFFDDE8FC)
        SectionAccent.SHOP -> Color(0xFFFCE6CC)
        SectionAccent.TASKS -> Color(0xFFE9DFFA)
        SectionAccent.SAVINGS -> Color(0xFFDDF1E1)
        SectionAccent.PROGRESS -> Color(0xFFD8EFEB)
        SectionAccent.ADULT -> Color(0xFFE6E9EC)
    }
}

/** Панели и ряд вкладок внутри раздела — на тон глубже фона (в тёмной теме — светлее). */
fun sectionPanel(accent: SectionAccent): Color = if (FinnyColors.isDark()) {
    when (accent) {
        SectionAccent.PLAN -> Color(0xFF24314A)
        SectionAccent.SHOP -> Color(0xFF3E2D1B)
        SectionAccent.TASKS -> Color(0xFF33284A)
        SectionAccent.SAVINGS -> Color(0xFF20392A)
        SectionAccent.PROGRESS -> Color(0xFF1C3934)
        SectionAccent.ADULT -> Color(0xFF2E343A)
    }
} else {
    when (accent) {
        SectionAccent.PLAN -> Color(0xFFC9DAFA)
        SectionAccent.SHOP -> Color(0xFFF8D4AA)
        SectionAccent.TASKS -> Color(0xFFD9C9F5)
        SectionAccent.SAVINGS -> Color(0xFFC6E7CD)
        SectionAccent.PROGRESS -> Color(0xFFBFE3DD)
        SectionAccent.ADULT -> Color(0xFFD3D8DD)
    }
}

/**
 * Тема раздела. В тёмной теме карточки, дорожки и рамки берут оттенок раздела,
 * чтобы тёплые коричневые карточки не лежали на синем или зелёном фоне.
 */
@Composable
fun SectionTheme(accent: SectionAccent, content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme
    if (!FinnyColors.isDark()) {
        content()
        return
    }
    val (surface, border) = when (accent) {
        SectionAccent.PLAN -> Color(0xFF2A3752) to Color(0xFF3B4B6C)
        SectionAccent.SHOP -> Color(0xFF45331F) to Color(0xFF5B4631)
        SectionAccent.TASKS -> Color(0xFF3A2E54) to Color(0xFF4E406D)
        SectionAccent.SAVINGS -> Color(0xFF25402F) to Color(0xFF345740)
        SectionAccent.PROGRESS -> Color(0xFF21413B) to Color(0xFF2F5851)
        SectionAccent.ADULT -> Color(0xFF353C43) to Color(0xFF48505A)
    }
    MaterialTheme(
        colorScheme = base.copy(
            background = sectionBackground(accent),
            surface = surface,
            surfaceVariant = sectionPanel(accent),
            outline = lerp(border, Color.White, 0.18f),
            outlineVariant = border,
        ),
        content = content,
    )
}

@Composable
fun SectionHeader(
    accent: SectionAccent,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    petLook: PetLook = PetLook.Default,
    petStage: Int = 0,
) {
    val face = sectionAccentColor(accent)
    val edge = sectionAccentEdge(accent)
    val shape = MaterialTheme.shapes.extraLarge
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(edge),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-3).dp)
                .clip(shape)
                .background(face)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    color = FinnyColors.OnPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = hint,
                    color = FinnyColors.OnPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            PetSprite(
                look = petLook,
                stage = petStage,
                modifier = Modifier.size(72.dp),
            )
        }
    }
}
