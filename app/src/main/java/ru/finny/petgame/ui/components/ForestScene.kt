package ru.finny.petgame.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.data.entity.SavingsEntity
import ru.finny.petgame.ui.theme.FinnyColors

private enum class SceneItemState { HIDDEN, SAVING, OWNED }

/**
 * Место предмета на полянке. [x] — доля ширины от края по [alignment],
 * [lift] — подъём над нижним краем в долях размера питомца (для верхних — отступ от верха).
 */
private class ScenePlacement(
    val goalId: String,
    @DrawableRes val drawable: Int,
    @StringRes val name: Int,
    val alignment: Alignment,
    val x: Float,
    val lift: Float,
    val width: Float,
)

// Порядок = порядок отрисовки: дальние предметы раньше, передние позже.
private val scenePlacements = listOf(
    ScenePlacement("goal_kite", R.drawable.scene_kite, R.string.forest_item_kite, Alignment.TopStart, 0.06f, 0.02f, 0.2f),
    ScenePlacement("goal_swing", R.drawable.scene_swing, R.string.forest_item_swing, Alignment.BottomCenter, 0f, 0.95f, 0.3f),
    ScenePlacement("goal_house", R.drawable.scene_house, R.string.forest_item_house, Alignment.BottomStart, 0.02f, 0.42f, 0.4f),
    ScenePlacement("goal_slide", R.drawable.scene_slide, R.string.forest_item_slide, Alignment.BottomEnd, 0.02f, 0.4f, 0.38f),
    ScenePlacement("goal_flowers", R.drawable.scene_flowers, R.string.forest_item_flowers, Alignment.BottomStart, 0f, 0f, 0.27f),
)

@Composable
fun ForestBackground(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.scene_forest_bg),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
    )
}

/** Поляна с питомцем: купленные цели стоят рядом, копимая цель — полупрозрачный силуэт с подписью. */
@Composable
fun ForestMeadow(
    petName: String,
    achievedGoalIds: Set<String>,
    selectedGoalId: String?,
    savings: List<SavingsEntity>,
    pet: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val savingRow = savings.firstOrNull { it.goalId == selectedGoalId }
    val owned = scenePlacements.filter { it.goalId in achievedGoalIds }.map { stringResource(it.name) }
    val description = if (owned.isEmpty()) {
        stringResource(R.string.forest_desc_pet, petName)
    } else {
        stringResource(R.string.forest_desc_pet_with, petName, owned.joinToString())
    }

    BoxWithConstraints(modifier = modifier.semantics { contentDescription = description }) {
        val w = maxWidth
        val petSize = minOf(w * 0.5f, maxHeight * 0.62f)
        scenePlacements.forEach { place ->
            val state = when {
                place.goalId in achievedGoalIds -> SceneItemState.OWNED
                place.goalId == selectedGoalId -> SceneItemState.SAVING
                else -> SceneItemState.HIDDEN
            }
            if (state == SceneItemState.HIDDEN) return@forEach
            val top = place.alignment == Alignment.TopStart
            val dx = when (place.alignment) {
                Alignment.BottomEnd -> -w * place.x
                else -> w * place.x
            }
            val dy = if (top) maxHeight * place.lift else -petSize * place.lift
            SceneItem(
                drawable = place.drawable,
                saving = state == SceneItemState.SAVING,
                width = w * place.width,
                label = savingRow?.takeIf { state == SceneItemState.SAVING }?.let {
                    stringResource(R.string.forest_saving_label, it.savedAmount.toInt(), it.goalCost.toInt())
                },
                modifier = Modifier
                    .align(place.alignment)
                    .offset(x = dx, y = dy),
            )
        }
        pet(
            Modifier
                .align(Alignment.BottomCenter)
                .size(petSize),
        )
    }
}

private val ghostFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
private fun SceneItem(
    @DrawableRes drawable: Int,
    saving: Boolean,
    width: Dp,
    label: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(drawable),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = if (saving) ghostFilter else null,
            modifier = Modifier
                .width(width)
                .alpha(if (saving) 0.45f else 1f),
        )
        if (label != null) {
            ScenePlate(text = label, small = true, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
fun ScenePlate(text: String, modifier: Modifier = Modifier, small: Boolean = false) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(FinnyColors.SplashBarTrack.copy(alpha = 0.94f))
            .border(2.dp, FinnyColors.SplashInk, shape)
            .padding(horizontal = if (small) 12.dp else 18.dp, vertical = if (small) 4.dp else 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = FinnyColors.SplashInk,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = if (small) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
fun CoinIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(FinnyColors.ProgressYellow, r, c)
        drawCircle(FinnyColors.SplashBarFill, r * 0.62f, c)
        drawCircle(FinnyColors.ProgressHighlight, r * 0.2f, Offset(c.x - r * 0.25f, c.y - r * 0.25f))
        drawCircle(FinnyColors.SplashInk, r - size.minDimension * 0.05f, c, style = Stroke(size.minDimension * 0.1f))
    }
}
