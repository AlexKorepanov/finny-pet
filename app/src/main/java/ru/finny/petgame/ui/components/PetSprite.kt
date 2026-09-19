package ru.finny.petgame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

private val PET_COLORS = listOf(
    Color(0xFFF9A825),
    Color(0xFFB0BEC5),
    Color(0xFFF48FB1),
)

@Composable
fun PetSprite(species: Int, colorIndex: Int, modifier: Modifier = Modifier) {
    val color = PET_COLORS[colorIndex.coerceIn(PET_COLORS.indices)]
    Canvas(modifier = modifier) {
        drawPet(species = species.coerceIn(0, 2), color = color)
    }
}

private fun Color.darkened(): Color =
    Color(red = red * 0.6f, green = green * 0.6f, blue = blue * 0.6f, alpha = alpha)

private fun DrawScope.drawPet(species: Int, color: Color) {
    val dark = color.darkened()
    val u = minOf(size.width, size.height)
    val cx = size.width / 2f
    val headR = u * 0.26f
    val headCenter = Offset(cx, size.height * 0.40f)

    val tailRadius = when (species) {
        0 -> u * 0.05f
        1 -> u * 0.08f
        else -> u * 0.10f
    }
    drawCircle(color = color, radius = tailRadius, center = Offset(size.width * 0.82f, size.height * 0.76f))

    drawRoundRect(
        color = color,
        topLeft = Offset(cx - u * 0.23f, size.height * 0.56f),
        size = Size(u * 0.46f, u * 0.34f),
        cornerRadius = CornerRadius(u * 0.17f, u * 0.17f),
    )

    drawOval(
        color = Color.White.copy(alpha = 0.45f),
        topLeft = Offset(cx - u * 0.09f, size.height * 0.64f),
        size = Size(u * 0.18f, u * 0.20f),
    )

    when (species) {
        0 -> {
            drawPath(
                path = Path().apply {
                    moveTo(headCenter.x - headR * 0.95f, headCenter.y - headR * 0.30f)
                    lineTo(headCenter.x - headR * 0.35f, headCenter.y - headR * 1.35f)
                    lineTo(headCenter.x - headR * 0.10f, headCenter.y - headR * 0.50f)
                    close()
                },
                color = color,
            )
            drawPath(
                path = Path().apply {
                    moveTo(headCenter.x + headR * 0.95f, headCenter.y - headR * 0.30f)
                    lineTo(headCenter.x + headR * 0.35f, headCenter.y - headR * 1.35f)
                    lineTo(headCenter.x + headR * 0.10f, headCenter.y - headR * 0.50f)
                    close()
                },
                color = color,
            )
        }
        1 -> {
            val earW = u * 0.13f
            val earH = u * 0.26f
            withTransform({
                rotate(degrees = -30f, pivot = Offset(headCenter.x - headR * 0.85f, headCenter.y))
            }) {
                drawOval(
                    color = color,
                    topLeft = Offset(headCenter.x - headR * 0.85f - earW / 2f, headCenter.y - earH * 0.55f),
                    size = Size(earW, earH),
                )
            }
            withTransform({
                rotate(degrees = 30f, pivot = Offset(headCenter.x + headR * 0.85f, headCenter.y))
            }) {
                drawOval(
                    color = color,
                    topLeft = Offset(headCenter.x + headR * 0.85f - earW, headCenter.y - earH * 0.55f),
                    size = Size(earW, earH),
                )
            }
        }
        else -> {
            val earW = u * 0.11f
            val earH = u * 0.30f
            drawOval(
                color = color,
                topLeft = Offset(headCenter.x - headR * 0.55f, headCenter.y - u * 0.34f),
                size = Size(earW, earH),
            )
            drawOval(
                color = color,
                topLeft = Offset(headCenter.x + headR * 0.10f, headCenter.y - u * 0.34f),
                size = Size(earW, earH),
            )
        }
    }

    drawCircle(color = color, radius = headR, center = headCenter)

    val eyeY = headCenter.y - headR * 0.10f
    val eyeOffset = headR * 0.42f
    drawCircle(color = Color.White, radius = u * 0.052f, center = Offset(headCenter.x - eyeOffset, eyeY))
    drawCircle(color = Color.White, radius = u * 0.052f, center = Offset(headCenter.x + eyeOffset, eyeY))
    drawCircle(
        color = Color(0xFF263238),
        radius = u * 0.026f,
        center = Offset(headCenter.x - eyeOffset + u * 0.012f, eyeY + u * 0.008f),
    )
    drawCircle(
        color = Color(0xFF263238),
        radius = u * 0.026f,
        center = Offset(headCenter.x + eyeOffset + u * 0.012f, eyeY + u * 0.008f),
    )

    drawCircle(color = dark, radius = u * 0.018f, center = Offset(headCenter.x, headCenter.y + headR * 0.25f))

    drawArc(
        color = dark,
        startAngle = 25f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(headCenter.x - u * 0.05f, headCenter.y + headR * 0.28f),
        size = Size(u * 0.10f, u * 0.09f),
        style = Stroke(width = 1.5.dp.toPx()),
    )

    if (species == 0) {
        val whiskerY = headCenter.y + headR * 0.15f
        drawLine(
            color = dark,
            start = Offset(headCenter.x - headR * 0.55f, whiskerY),
            end = Offset(headCenter.x - headR - u * 0.05f, whiskerY - u * 0.02f),
            strokeWidth = 1.dp.toPx(),
        )
        drawLine(
            color = dark,
            start = Offset(headCenter.x - headR * 0.55f, whiskerY + u * 0.015f),
            end = Offset(headCenter.x - headR - u * 0.05f, whiskerY + u * 0.02f),
            strokeWidth = 1.dp.toPx(),
        )
        drawLine(
            color = dark,
            start = Offset(headCenter.x + headR * 0.55f, whiskerY),
            end = Offset(headCenter.x + headR + u * 0.05f, whiskerY - u * 0.02f),
            strokeWidth = 1.dp.toPx(),
        )
        drawLine(
            color = dark,
            start = Offset(headCenter.x + headR * 0.55f, whiskerY),
            end = Offset(headCenter.x + headR + u * 0.05f, whiskerY - u * 0.02f),
            strokeWidth = 1.dp.toPx(),
        )
    }
}