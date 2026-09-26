package ru.finny.petgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.PetSprite
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.model.PetLook
import ru.finny.petgame.ui.theme.FinnyColors

/**
 * Раздел гардероба. Вкладка показывает увеличенную часть Финни, к которой относится раздел:
 * [focusX], [focusY] — точка на исходной картинке 420×445, [zoom] — увеличение.
 */
private enum class PetCustomizeTab(
    val titleRes: Int,
    val shortRes: Int,
    val focusX: Float,
    val focusY: Float,
    val zoom: Float,
) {
    HAT(R.string.pet_hat_title, R.string.pet_tab_hat, 210f, 110f, 2.1f),
    FACE(R.string.pet_face_title, R.string.pet_tab_face, 210f, 240f, 2.4f),
    OUTFIT(R.string.pet_outfit_title, R.string.pet_tab_outfit, 210f, 345f, 2.0f),
    EMOTION(R.string.pet_emotion_title, R.string.pet_tab_emotion, 210f, 235f, 2.1f),
    EYES(R.string.pet_eye_title, R.string.pet_tab_eyes, 210f, 236f, 3.2f),
    ;

    /** Как выглядит Финни на вкладке: если вещь не надета, показываем пример. */
    fun sample(look: PetLook): PetLook = when (this) {
        HAT -> if (look.hat == 0) look.withHat(3) else look
        FACE -> if (look.face == 0) look.withFace(2) else look
        OUTFIT -> if (look.outfit == 0) look.withOutfit(1) else look
        EMOTION, EYES -> look.withFace(0)
    }
}

@Composable
fun PetScreen(
    look: PetLook,
    petName: String,
    onLookChange: (PetLook) -> Unit,
    onPetNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onHint: () -> Unit,
    onNext: () -> Unit,
    editMode: Boolean = false,
    festiveUnlocked: Boolean = false,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tab = PetCustomizeTab.entries[selectedTab.coerceIn(0, PetCustomizeTab.entries.lastIndex)]

    val labels: List<String> = when (tab) {
        PetCustomizeTab.HAT -> stringArrayResource(R.array.pet_hat_labels).toList()
        PetCustomizeTab.FACE -> stringArrayResource(R.array.pet_face_labels).toList()
        PetCustomizeTab.OUTFIT -> {
            val base = stringArrayResource(R.array.pet_outfit_labels).toList()
            if (festiveUnlocked) base + stringResource(R.string.pet_outfit_festive) else base
        }
        PetCustomizeTab.EMOTION -> stringArrayResource(R.array.pet_emotion_labels).toList()
        PetCustomizeTab.EYES -> stringArrayResource(R.array.pet_eye_labels).toList()
    }
    val selectedIndex = when (tab) {
        PetCustomizeTab.HAT -> look.hat
        PetCustomizeTab.FACE -> look.face
        PetCustomizeTab.OUTFIT -> look.outfit
        PetCustomizeTab.EMOTION -> look.emotion
        PetCustomizeTab.EYES -> look.eyeColor
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(
                    if (editMode) R.string.wardrobe_title else R.string.pet_title,
                ),
                showBack = true,
                onBack = onBack,
                onHint = onHint,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            // Превью всегда наверху — не уезжает при выборе аксессуаров
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FinnyColors.SoftBlue)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                PetSprite(look = look, modifier = Modifier.size(176.dp))
            }

            // Иконки разделов — горизонтальный слайдер (как в Duolingo)
            CategoryTabRow(
                look = look,
                selected = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FinnyColors.SurfaceVariant)
                    .padding(vertical = 10.dp),
            )

            Text(
                text = stringResource(tab.titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = FinnyColors.TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )

            // Сетка вариантов только для текущего раздела
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(labels) { index, label ->
                    AccessoryTile(
                        label = label,
                        selected = index == selectedIndex,
                        previewLook = when (tab) {
                            PetCustomizeTab.HAT -> look.withHat(index)
                            PetCustomizeTab.FACE -> look.withFace(index)
                            PetCustomizeTab.OUTFIT -> look.withOutfit(index)
                            PetCustomizeTab.EMOTION -> look.withEmotion(index)
                            PetCustomizeTab.EYES -> look.withFace(0).withEyeColor(index)
                        },
                        onClick = {
                            onLookChange(
                                when (tab) {
                                    PetCustomizeTab.HAT -> look.withHat(index)
                                    PetCustomizeTab.FACE -> look.withFace(index)
                                    PetCustomizeTab.OUTFIT -> look.withOutfit(index)
                                    PetCustomizeTab.EMOTION -> look.withEmotion(index)
                                    PetCustomizeTab.EYES -> look.withEyeColor(index)
                                },
                            )
                        },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FinnyColors.Surface)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = petName,
                    onValueChange = { value -> if (value.length <= 20) onPetNameChange(value) },
                    label = { Text(text = stringResource(R.string.pet_name_label)) },
                    placeholder = { Text(text = stringResource(R.string.pet_name_hint)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                PrimaryButton(
                    text = stringResource(if (editMode) R.string.save else R.string.next),
                    onClick = onNext,
                    enabled = petName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CategoryTabRow(
    look: PetLook,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PetCustomizeTab.entries.forEachIndexed { index, tab ->
            CategoryTab(
                tab = tab,
                look = look,
                selected = index == selected,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CategoryTab(
    tab: PetCustomizeTab,
    look: PetLook,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val border = if (selected) FinnyColors.Primary else FinnyColors.CardBorder
    val bg = if (selected) FinnyColors.SoftBlue else FinnyColors.Surface
    Column(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(width = if (selected) 3.dp else 2.dp, color = border, shape = shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(top = 6.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PetCloseUp(look = tab.sample(look), tab = tab, modifier = Modifier.size(44.dp))
        Text(
            text = stringResource(tab.shortRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) FinnyColors.Primary else FinnyColors.TextPrimary,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** Увеличенный кусочек Финни вокруг точки раздела. */
@Composable
private fun PetCloseUp(look: PetLook, tab: PetCustomizeTab, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.clip(RoundedCornerShape(10.dp)).clipToBounds()) {
        val box = maxWidth
        val sprite = box * tab.zoom
        val scale = sprite / 445f
        val spriteLeft = (sprite - scale * 420f) / 2f
        val dx = box / 2f - (spriteLeft + scale * tab.focusX) + (sprite - box) / 2f
        val dy = box / 2f - scale * tab.focusY + (sprite - box) / 2f
        PetSprite(
            look = look,
            stage = 2,
            modifier = Modifier
                .requiredSize(sprite)
                .offset(x = dx, y = dy),
        )
    }
}

@Composable
private fun AccessoryTile(
    label: String,
    selected: Boolean,
    previewLook: PetLook,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val border = if (selected) FinnyColors.Primary else FinnyColors.CardBorder
    val bg = if (selected) FinnyColors.SoftBlue else FinnyColors.Surface
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.95f)
            .clip(shape)
            .background(bg)
            .border(width = if (selected) 3.dp else 2.dp, color = border, shape = shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PetSprite(
            look = previewLook,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = FinnyColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
