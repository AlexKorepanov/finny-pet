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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.finny.petgame.R
import ru.finny.petgame.ui.theme.FinnyColors

/** Полоса загрузки как на стартовом экране: капсула, кремовый трек, оранжевая заливка. */
@Composable
fun SplashLoadingBlock(
    progress: Float,
    modifier: Modifier = Modifier,
) {
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
            SplashLeaf(flip = false)
            Text(
                text = stringResource(R.string.splash_slogan),
                color = FinnyColors.SplashInk,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp,
                ),
                textAlign = TextAlign.Center,
            )
            SplashLeaf(flip = true)
        }
        SplashLoadingBar(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth(0.61f),
        )
        Text(
            text = stringResource(R.string.splash_loading),
            color = FinnyColors.SplashInk,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 25.sp,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SplashLoadingBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50)
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(shape)
            .background(FinnyColors.SplashBarTrack)
            .border(width = 2.dp, color = FinnyColors.SplashInk, shape = shape),
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(FinnyColors.SplashBarFill),
            )
        }
    }
}

@Composable
private fun SplashLeaf(flip: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 18.dp, height = 14.dp)) {
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
        drawPath(leaf, color = FinnyColors.SplashInk, style = Fill)
        val veinStart = if (flip) Offset(w * 0.85f, h * 0.45f) else Offset(w * 0.15f, h * 0.45f)
        val veinEnd = if (flip) Offset(w * 0.25f, h * 0.4f) else Offset(w * 0.75f, h * 0.4f)
        drawLine(
            color = FinnyColors.SplashInk.copy(alpha = 0.35f),
            start = veinStart,
            end = veinEnd,
            strokeWidth = 1.5f,
        )
    }
}
