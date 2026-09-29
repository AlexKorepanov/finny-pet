package ru.finny.petgame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.finny.petgame.R
import ru.finny.petgame.ui.theme.LocalTimeOfDay

/** Название игры поверх картинки заставки: лапка, «Питомец Финни» между веточками, сердечко. */
@Composable
fun SplashTitle(modifier: Modifier = Modifier) {
    val time = LocalTimeOfDay.current
    val ink = time.splashInk
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SplashPaw(color = ink)
        Text(
            text = stringResource(R.string.splash_title_top),
            style = splashText(ink, time.splashTextShadow, 40.sp, FontWeight.ExtraBold),
            textAlign = TextAlign.Center,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SplashLeaf(flip = false, color = ink, modifier = Modifier.size(width = 26.dp, height = 20.dp))
            Text(
                text = stringResource(R.string.splash_title_bottom),
                style = splashText(ink, time.splashTextShadow, 40.sp, FontWeight.ExtraBold),
                textAlign = TextAlign.Center,
            )
            SplashLeaf(flip = true, color = ink, modifier = Modifier.size(width = 26.dp, height = 20.dp))
        }
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Полоса загрузки как на стартовом экране: капсула, трек и заливка в цветах времени суток. */
@Composable
fun SplashLoadingBlock(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val time = LocalTimeOfDay.current
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SplashLeaf(flip = false, color = time.splashInk)
            Text(
                text = stringResource(R.string.splash_slogan),
                style = splashText(time.splashInk, time.splashTextShadow, 18.sp, FontWeight.SemiBold),
                textAlign = TextAlign.Center,
            )
            SplashLeaf(flip = true, color = time.splashInk)
        }
        SplashLoadingBar(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth(0.61f),
        )
        Text(
            text = stringResource(R.string.splash_loading),
            style = splashText(time.splashInk, time.splashTextShadow, 25.sp, FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SplashLoadingBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val time = LocalTimeOfDay.current
    val shape = RoundedCornerShape(percent = 50)
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(shape)
            .background(time.splashBarTrack)
            .border(width = 2.dp, color = time.splashBarBorder, shape = shape),
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(time.splashBarFill),
            )
        }
    }
}

@Composable
private fun splashText(color: Color, shadow: Color, size: TextUnit, weight: FontWeight): TextStyle =
    MaterialTheme.typography.titleMedium.copy(
        color = color,
        fontSize = size,
        lineHeight = size * 1.1f,
        fontWeight = weight,
        letterSpacing = 0.3.sp,
        shadow = Shadow(color = shadow, offset = Offset(0f, 2f), blurRadius = 10f),
    )

@Composable
private fun SplashPaw(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        drawOval(color, topLeft = Offset(w * 0.24f, h * 0.46f), size = Size(w * 0.52f, h * 0.44f))
        listOf(0.14f to 0.36f, 0.34f to 0.14f, 0.62f to 0.14f, 0.84f to 0.36f).forEach { (x, y) ->
            drawCircle(color, radius = w * 0.11f, center = Offset(w * x, h * y))
        }
    }
}

@Composable
private fun SplashLeaf(flip: Boolean, color: Color, modifier: Modifier = Modifier.size(width = 18.dp, height = 14.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val leaf = Path().apply {
            if (!flip) {
                moveTo(0f, h * 0.55f)
                cubicTo(w * 0.15f, h * 0.05f, w * 0.55f, 0f, w * 0.95f, h * 0.35f)
                cubicTo(w * 0.7f, h * 0.55f, w * 0.35f, h * 0.95f, 0f, h * 0.55f)
                close()
            } else {
                moveTo(w, h * 0.55f)
                cubicTo(w * 0.85f, h * 0.05f, w * 0.45f, 0f, w * 0.05f, h * 0.35f)
                cubicTo(w * 0.3f, h * 0.55f, w * 0.65f, h * 0.95f, w, h * 0.55f)
                close()
            }
        }
        drawPath(leaf, color = color, style = Fill)
        val veinStart = if (flip) Offset(w * 0.85f, h * 0.45f) else Offset(w * 0.15f, h * 0.45f)
        val veinEnd = if (flip) Offset(w * 0.25f, h * 0.4f) else Offset(w * 0.75f, h * 0.4f)
        drawLine(
            color = color.copy(alpha = 0.35f),
            start = veinStart,
            end = veinEnd,
            strokeWidth = 1.5f,
        )
    }
}
