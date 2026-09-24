package ru.finny.petgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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

private enum class PetCustomizeTab(
    val titleRes: Int,
    val icon: ImageVector,
) {
    HAT(R.string.pet_hat_title, Icons.Filled.Star),
    FACE(R.string.pet_face_title, Icons.Filled.Info),
    OUTFIT(R.string.pet_outfit_title, Icons.Filled.Home),
    EMOTION(R.string.pet_emotion_title, Icons.Filled.Favorite),
    EYES(R.string.pet_eye_title, Icons.Filled.Face),
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
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tab = PetCustomizeTab.entries[selectedTab.coerceIn(0, PetCustomizeTab.entries.lastIndex)]

    val labels = when (tab) {
        PetCustomizeTab.HAT -> stringArrayResource(R.array.pet_hat_labels)
        PetCustomizeTab.FACE -> stringArrayResource(R.array.pet_face_labels)
        PetCustomizeTab.OUTFIT -> stringArrayResource(R.array.pet_outfit_labels)
        PetCustomizeTab.EMOTION -> stringArrayResource(R.array.pet_emotion_labels)
        PetCustomizeTab.EYES -> stringArrayResource(R.array.pet_eye_labels)
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
                title = stringResource(R.string.pet_title),
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
                itemsIndexed(labels.toList()) { index, label ->
                    AccessoryTile(
                        label = label,
                        selected = index == selectedIndex,
                        previewLook = when (tab) {
                            PetCustomizeTab.HAT -> look.withHat(index)
                            PetCustomizeTab.FACE -> look.withFace(index)
                            PetCustomizeTab.OUTFIT -> look.withOutfit(index)
                            PetCustomizeTab.EMOTION -> look.withEmotion(index)
                            PetCustomizeTab.EYES -> look.withEyeColor(index)
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
                    text = stringResource(R.string.next),
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
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PetCustomizeTab.entries.forEachIndexed { index, tab ->
            CategoryTab(
                icon = tab.icon,
                selected = index == selected,
                onClick = { onSelect(index) },
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
    }
}

@Composable
private fun CategoryTab(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val border = if (selected) FinnyColors.Primary else FinnyColors.CardBorder
    val bg = if (selected) FinnyColors.SoftBlue else FinnyColors.Surface
    val tint = if (selected) FinnyColors.Primary else FinnyColors.TextSecondary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .border(width = if (selected) 3.dp else 2.dp, color = border, shape = RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp),
            )
        }
        if (selected) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(FinnyColors.Primary),
            )
        } else {
            Spacer(modifier = Modifier.height(10.dp))
        }
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
