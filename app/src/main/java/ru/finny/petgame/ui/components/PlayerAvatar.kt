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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.cos
import kotlin.math.sin

private val STAR_COLOR = Color(0xFFFFD54F)
private val ROCKET_BODY = Color(0xFFECEFF1)
private val ROCKET_ACCENT = Color(0xFFE57373)
private val ROCKET_WINDOW = Color(0xFF64B5F6)
private val ROBOT_HEAD = Color(0xFFB0BEC5)
private val ROBOT_ACCENT = Color(0xFF78909C)
private val DARK = Color(0xFF263238)

@Composable
fun PlayerAvatar(
    index: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    Canvas(modifier = modifier.then(semanticsModifier)) {
        when (index.coerceIn(0, 2)) {
            0 -> drawStar()
            1 -> drawRocket()
            else -> drawRobot()
        }
    }
}

private fun DrawScope.drawStar() {
    val u = minOf(size.width, size.height)
    val cx = size.width / 2f
    val cy = size.height / 2f
    val outer = u * 0.40f
    val inner = u * 0.16f
    val path = Path()
    for (i in 0 until 10) {
        val angle = -Math.PI / 2 + i * Math.PI / 5
        val radius = if (i % 2 == 0) outer else inner
        val x = cx + (radius * cos(angle)).toFloat()
        val y = cy + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = STAR_COLOR)
}

private fun DrawScope.drawRocket() {
    val u = minOf(size.width, size.height)
    val cx = size.width / 2f
    drawPath(
        path = Path().apply {
            moveTo(cx - u * 0.07f, size.height * 0.72f)
            lineTo(cx, size.height * 0.94f)
            lineTo(cx + u * 0.07f, size.height * 0.72f)
            close()
        },
        color = Color(0xFFFFB74D),
    )
    drawOval(
        color = ROCKET_BODY,
        topLeft = Offset(cx - u * 0.13f, size.height * 0.18f),
        size = Size(u * 0.26f, u * 0.55f),
    )
    drawPath(
        path = Path().apply {
            moveTo(cx - u * 0.115f, size.height * 0.30f)
            lineTo(cx, size.height * 0.06f)
            lineTo(cx + u * 0.115f, size.height * 0.30f)
            close()
        },
        color = ROCKET_ACCENT,
    )
    drawPath(
        path = Path().apply {
            moveTo(cx - u * 0.12f, size.height * 0.52f)
            lineTo(cx - u * 0.22f, size.height * 0.76f)
            lineTo(cx - u * 0.12f, size.height * 0.72f)
            close()
        },
        color = ROCKET_ACCENT,
    )
    drawPath(
        path = Path().apply {
            moveTo(cx + u * 0.12f, size.height * 0.52f)
            lineTo(cx + u * 0.22f, size.height * 0.76f)
            lineTo(cx + u * 0.12f, size.height * 0.72f)
            close()
        },
        color = ROCKET_ACCENT,
    )
    drawCircle(color = ROCKET_WINDOW, radius = u * 0.07f, center = Offset(cx, size.height * 0.42f))
    drawCircle(color = Color.White, radius = u * 0.04f, center = Offset(cx, size.height * 0.42f))
}

private fun DrawScope.drawRobot() {
    val u = minOf(size.width, size.height)
    val cx = size.width / 2f
    drawLine(
        color = ROBOT_ACCENT,
        start = Offset(cx, size.height * 0.26f),
        end = Offset(cx, size.height * 0.14f),
        strokeWidth = u * 0.03f,
    )
    drawCircle(color = ROCKET_ACCENT, radius = u * 0.035f, center = Offset(cx, size.height * 0.12f))
    drawCircle(color = ROBOT_ACCENT, radius = u * 0.045f, center = Offset(cx - u * 0.30f, size.height * 0.48f))
    drawCircle(color = ROBOT_ACCENT, radius = u * 0.045f, center = Offset(cx + u * 0.30f, size.height * 0.48f))
    drawRoundRect(
        color = ROBOT_HEAD,
        topLeft = Offset(cx - u * 0.26f, size.height * 0.26f),
        size = Size(u * 0.52f, u * 0.44f),
        cornerRadius = CornerRadius(u * 0.10f, u * 0.10f),
    )
    drawCircle(color = Color.White, radius = u * 0.07f, center = Offset(cx - u * 0.11f, size.height * 0.42f))
    drawCircle(color = Color.White, radius = u * 0.07f, center = Offset(cx + u * 0.11f, size.height * 0.42f))
    drawCircle(color = DARK, radius = u * 0.035f, center = Offset(cx - u * 0.11f, size.height * 0.42f))
    drawCircle(color = DARK, radius = u * 0.035f, center = Offset(cx + u * 0.11f, size.height * 0.42f))
    drawRoundRect(
        color = ROBOT_ACCENT,
        topLeft = Offset(cx - u * 0.09f, size.height * 0.54f),
        size = Size(u * 0.18f, u * 0.05f),
        cornerRadius = CornerRadius(u * 0.02f, u * 0.02f),
    )
}