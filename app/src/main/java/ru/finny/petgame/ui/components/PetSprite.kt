package ru.finny.petgame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import kotlin.math.sqrt

private val PET_COLORS = listOf(
    Color(0xFFF9A825),
    Color(0xFFB0BEC5),
    Color(0xFFF48FB1),
)

private val DARK_NEUTRAL = Color(0xFF263238)
private val DOG_NOSE = Color(0xFF4E342E)
private val DOG_TONGUE = Color(0xFFE57373)
private val BLUSH = Color(0xFFE57373)
private val NOSE_PINK = Color(0xFFF06292)
private val INNER_EAR_PINK = lerp(Color(0xFFF06292), Color.White, 0.4f)

@Composable
fun PetSprite(species: Int, colorIndex: Int, modifier: Modifier = Modifier) {
    val color = PET_COLORS[colorIndex.coerceIn(PET_COLORS.indices)]
    Canvas(modifier = modifier) {
        drawPet(species = species.coerceIn(0, 2), color = color)
    }
}

private class PetPalette(val base: Color) {
    val outline = base.scaled(0.8f)
    val earDark = base.scaled(0.85f)
    val topLight = lerp(base, Color.White, 0.35f)
    val light = lerp(base, Color.White, 0.55f)
    val innerEar = lerp(base, Color.White, 0.7f)
}

private fun Color.scaled(factor: Float): Color =
    Color(red = red * factor, green = green * factor, blue = blue * factor, alpha = 1f)

private fun DrawScope.drawPet(species: Int, color: Color) {
    val palette = PetPalette(color)
    when (species) {
        0 -> drawCat(palette)
        1 -> drawDog(palette)
        else -> drawRabbit(palette)
    }
}

private fun DrawScope.drawCat(p: PetPalette) {
    val u = minOf(size.width, size.height)
    val h = size.height
    val cx = size.width / 2f
    val headCy = h * 0.36f
    val line = u * 0.012f

    drawPetShadow(cx, u)
    drawTaperedTail(catTailPath(cx, u, h), p, line, lightTipCenter = null, lightTipRadius = 0f)
    drawPetOval(p, cx, h * 0.685f, u * 0.23f, u * 0.21f, line)
    drawPetBelly(p, cx, h * 0.70f, u * 0.13f, u * 0.15f)
    drawPetPaws(p, cx, u, h * 0.86f, line)

    listOf(-1f, 1f).forEach { side ->
        val ear = catEarPath(cx, headCy, u, side)
        drawPath(ear, color = p.base)
        drawPath(ear, color = p.outline, style = Stroke(width = line))
        drawPath(catInnerEarPath(cx, headCy, u, side), color = INNER_EAR_PINK)
    }

    drawPetOval(p, cx, headCy, u * 0.27f, u * 0.255f, line)
    drawPetMuzzle(p, cx, headCy + 0.10f * u, u * 0.13f, u * 0.105f)
    drawPetBlush(cx, headCy + 0.075f * u, 0.185f * u, u)
    drawPetEyes(cx, headCy - 0.05f * u, 0.105f * u, u)

    val nose = roundedTrianglePath(cx, headCy + 0.012f * u, u * 0.024f, u * 0.032f, u * 0.009f)
    drawPath(nose, color = NOSE_PINK)
    drawPath(nose, color = NOSE_PINK.scaled(0.75f), style = Stroke(width = u * 0.008f))

    drawArc(
        color = DARK_NEUTRAL,
        startAngle = 25f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(cx - 0.040f * u, headCy + 0.054f * u),
        size = Size(0.080f * u, 0.070f * u),
        style = Stroke(width = u * 0.010f, cap = StrokeCap.Round),
    )

    drawCatWhiskers(p, cx, headCy, u)
}

private fun DrawScope.drawDog(p: PetPalette) {
    val u = minOf(size.width, size.height)
    val h = size.height
    val cx = size.width / 2f
    val headCy = h * 0.36f
    val line = u * 0.012f

    drawPetShadow(cx, u)

    val tail = dogTailPath(cx, u, h)
    drawTaperedTail(
        tail,
        p,
        line,
        lightTipCenter = Offset(cx + 0.365f * u, h * 0.410f),
        lightTipRadius = u * 0.040f,
    )

    drawPetOval(p, cx, h * 0.685f, u * 0.23f, u * 0.21f, line)
    drawPetBelly(p, cx, h * 0.70f, u * 0.13f, u * 0.15f)
    drawPetPaws(p, cx, u, h * 0.86f, line)

    listOf(-1f, 1f).forEach { side ->
        val ear = dogEarPath(cx, headCy, u, side)
        drawPath(ear, color = p.earDark)
        drawPath(ear, color = p.outline, style = Stroke(width = line))
        drawPath(dogInnerEarPath(cx, headCy, u, side), color = p.innerEar)
    }

    drawPetOval(p, cx, headCy, u * 0.27f, u * 0.255f, line)
    drawPetMuzzle(p, cx, headCy + 0.115f * u, u * 0.165f, u * 0.135f)
    drawPetBlush(cx, headCy + 0.075f * u, 0.185f * u, u)
    drawPetEyes(cx, headCy - 0.05f * u, 0.105f * u, u)

    val nose = roundedTrianglePath(cx, headCy + 0.01f * u, u * 0.042f, u * 0.052f, u * 0.014f)
    drawPath(nose, color = DOG_NOSE)
    drawPath(nose, color = DOG_NOSE.scaled(0.8f), style = Stroke(width = u * 0.010f))
    drawOval(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(cx - 0.026f * u, headCy + 0.016f * u),
        size = Size(0.024f * u, 0.014f * u),
    )

    val mouthTop = headCy + 0.078f * u
    val mouth = petMouthPath(cx, mouthTop, u * 0.055f, u * 0.062f)
    drawPath(mouth, color = DARK_NEUTRAL)
    clipPath(mouth) {
        drawOval(
            color = DOG_TONGUE,
            topLeft = Offset(cx - 0.045f * u, mouthTop + 0.018f * u),
            size = Size(0.09f * u, 0.09f * u),
        )
    }
    drawPath(mouth, color = DARK_NEUTRAL, style = Stroke(width = u * 0.010f))
}

private fun DrawScope.drawRabbit(p: PetPalette) {
    val u = minOf(size.width, size.height)
    val h = size.height
    val cx = size.width / 2f
    val headCy = h * 0.36f
    val line = u * 0.012f

    drawPetShadow(cx, u)

    val pompon = ovalPath(cx + 0.265f * u, h * 0.73f, u * 0.075f, u * 0.075f)
    drawPath(pompon, color = p.base)
    clipPath(pompon) {
        drawOval(
            color = p.light,
            topLeft = Offset(cx + 0.235f * u, h * 0.700f),
            size = Size(0.05f * u, 0.042f * u),
        )
    }
    drawPath(pompon, color = p.outline, style = Stroke(width = line))

    drawPetOval(p, cx, h * 0.685f, u * 0.23f, u * 0.21f, line)
    drawPetBelly(p, cx, h * 0.70f, u * 0.13f, u * 0.15f)
    drawPetPaws(p, cx, u, h * 0.86f, line)

    listOf(-1f, 1f).forEach { side ->
        val earCx = cx + side * 0.085f * u
        val ear = ovalPath(earCx, headCy - 0.17f * u, u * 0.052f, u * 0.17f)
        drawPath(ear, color = p.base)
        drawPath(ear, color = p.outline, style = Stroke(width = line))
        drawPath(
            ovalPath(earCx, headCy - 0.19f * u, u * 0.026f, u * 0.115f),
            color = INNER_EAR_PINK,
        )
    }

    drawPetOval(p, cx, headCy, u * 0.27f, u * 0.255f, line)
    drawPetMuzzle(p, cx, headCy + 0.105f * u, u * 0.125f, u * 0.10f)
    drawPetBlush(cx, headCy + 0.075f * u, 0.185f * u, u)
    drawPetEyes(cx, headCy - 0.05f * u, 0.105f * u, u)

    val heart = heartPath(cx, headCy + 0.012f * u, u)
    drawPath(heart, color = NOSE_PINK)
    drawPath(heart, color = NOSE_PINK.scaled(0.75f), style = Stroke(width = u * 0.008f))

    val mouthTop = headCy + 0.058f * u
    val mouth = petMouthPath(cx, mouthTop, u * 0.035f, u * 0.050f)
    drawPath(mouth, color = DARK_NEUTRAL)
    clipPath(mouth) {
        val toothW = 0.015f * u
        val toothH = 0.024f * u
        val gap = 0.004f * u
        val teethTop = mouthTop + 0.005f * u
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(cx - gap / 2f - toothW, teethTop),
            size = Size(toothW, toothH),
            cornerRadius = CornerRadius(u * 0.004f, u * 0.004f),
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(cx + gap / 2f, teethTop),
            size = Size(toothW, toothH),
            cornerRadius = CornerRadius(u * 0.004f, u * 0.004f),
        )
    }
    drawPath(mouth, color = DARK_NEUTRAL, style = Stroke(width = u * 0.010f))
}

private fun DrawScope.drawPetShadow(cx: Float, u: Float) {
    drawOval(
        color = Color.Black.copy(alpha = 0.10f),
        topLeft = Offset(cx - 0.34f * u, size.height * 0.885f),
        size = Size(0.68f * u, 0.08f * u),
    )
}

private fun DrawScope.drawPetOval(p: PetPalette, cx: Float, cy: Float, rx: Float, ry: Float, line: Float) {
    val path = ovalPath(cx, cy, rx, ry)
    drawPath(
        path,
        brush = Brush.verticalGradient(
            colors = listOf(p.topLight, p.base),
            startY = cy - ry,
            endY = cy + ry,
        ),
    )
    drawPath(path, color = p.outline, style = Stroke(width = line))
}

private fun DrawScope.drawPetBelly(p: PetPalette, cx: Float, cy: Float, rx: Float, ry: Float) {
    drawOval(color = p.light, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2f, ry * 2f))
}

private fun DrawScope.drawPetPaws(p: PetPalette, cx: Float, u: Float, pawY: Float, line: Float) {
    listOf(-1f, 1f).forEach { side ->
        val pawCx = cx + side * 0.105f * u
        val paw = ovalPath(pawCx, pawY, u * 0.075f, u * 0.052f)
        drawPath(paw, color = p.light)
        drawPath(paw, color = p.outline, style = Stroke(width = line))
        listOf(-1f, 1f).forEach { toe ->
            drawArc(
                color = p.outline,
                startAngle = -75f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(pawCx + toe * 0.022f * u - 0.012f * u, pawY - 0.048f * u),
                size = Size(0.024f * u, 0.055f * u),
                style = Stroke(width = u * 0.008f, cap = StrokeCap.Round),
            )
        }
    }
}

private fun DrawScope.drawPetMuzzle(p: PetPalette, cx: Float, cy: Float, rx: Float, ry: Float) {
    val muzzle = ovalPath(cx, cy, rx, ry)
    drawPath(
        muzzle,
        brush = Brush.radialGradient(
            0.65f to p.light,
            1f to p.light.copy(alpha = 0f),
            center = Offset(cx, cy),
            radius = rx,
        ),
    )
}

private fun DrawScope.drawPetEyes(cx: Float, eyeY: Float, eyeOffset: Float, u: Float) {
    listOf(-1f, 1f).forEach { side ->
        val eyeX = cx + side * eyeOffset
        drawOval(
            color = DARK_NEUTRAL,
            topLeft = Offset(eyeX - 0.032f * u, eyeY - 0.032f * u),
            size = Size(0.064f * u, 0.064f * u),
        )
        drawCircle(
            color = Color.White,
            radius = u * 0.012f,
            center = Offset(eyeX - 0.010f * u, eyeY - 0.012f * u),
        )
        drawArc(
            color = DARK_NEUTRAL,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(eyeX - 0.042f * u, eyeY - 0.042f * u),
            size = Size(0.084f * u, 0.084f * u),
            style = Stroke(width = u * 0.010f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawPetBlush(cx: Float, y: Float, offset: Float, u: Float) {
    listOf(-1f, 1f).forEach { side ->
        drawCircle(
            color = BLUSH.copy(alpha = 0.35f),
            radius = u * 0.034f,
            center = Offset(cx + side * offset, y),
        )
    }
}

private fun DrawScope.drawTaperedTail(
    tail: Path,
    p: PetPalette,
    line: Float,
    lightTipCenter: Offset?,
    lightTipRadius: Float,
) {
    drawPath(tail, color = p.base)
    if (lightTipCenter != null) {
        clipPath(tail) {
            drawOval(
                color = p.light,
                topLeft = Offset(lightTipCenter.x - lightTipRadius, lightTipCenter.y - lightTipRadius),
                size = Size(lightTipRadius * 2f, lightTipRadius * 2f),
            )
        }
    }
    drawPath(tail, color = p.outline, style = Stroke(width = line))
}

private fun DrawScope.drawCatWhiskers(p: PetPalette, cx: Float, headCy: Float, u: Float) {
    val y1 = headCy + 0.085f * u
    val y2 = headCy + 0.107f * u
    val width = u * 0.006f
    listOf(-1f, 1f).forEach { side ->
        drawLine(
            color = p.outline,
            start = Offset(cx + side * 0.105f * u, y1),
            end = Offset(cx + side * 0.185f * u, y1 - 0.016f * u),
            strokeWidth = width,
        )
        drawLine(
            color = p.outline,
            start = Offset(cx + side * 0.105f * u, y2),
            end = Offset(cx + side * 0.185f * u, y2 + 0.012f * u),
            strokeWidth = width,
        )
    }
}

private fun ovalPath(cx: Float, cy: Float, rx: Float, ry: Float): Path {
    val kx = rx * 0.5523f
    val ky = ry * 0.5523f
    return Path().apply {
        moveTo(cx, cy - ry)
        cubicTo(cx + kx, cy - ry, cx + rx, cy - ky, cx + rx, cy)
        cubicTo(cx + rx, cy + ky, cx + kx, cy + ry, cx, cy + ry)
        cubicTo(cx - kx, cy + ry, cx - rx, cy + ky, cx - rx, cy)
        cubicTo(cx - rx, cy - ky, cx - kx, cy - ry, cx, cy - ry)
        close()
    }
}

private fun petMouthPath(cx: Float, top: Float, halfWidth: Float, depth: Float): Path =
    Path().apply {
        moveTo(cx - halfWidth, top)
        cubicTo(
            cx - halfWidth * 0.55f, top + depth * 0.88f,
            cx - halfWidth * 0.80f, top + depth,
            cx, top + depth,
        )
        cubicTo(
            cx + halfWidth * 0.80f, top + depth,
            cx + halfWidth * 0.55f, top + depth * 0.88f,
            cx + halfWidth, top,
        )
        cubicTo(
            cx + halfWidth * 0.55f, top + depth * 0.13f,
            cx - halfWidth * 0.55f, top + depth * 0.13f,
            cx - halfWidth, top,
        )
        close()
    }

private fun roundedTrianglePath(cx: Float, topY: Float, halfWidth: Float, height: Float, round: Float): Path {
    val a = Offset(cx - halfWidth, topY)
    val b = Offset(cx + halfWidth, topY)
    val c = Offset(cx, topY + height)
    val abX = b.x - round
    val baX = a.x + round
    val acLen = sqrt((c.x - a.x) * (c.x - a.x) + (c.y - a.y) * (c.y - a.y))
    val a2 = Offset(a.x + (c.x - a.x) / acLen * round, a.y + (c.y - a.y) / acLen * round)
    val b2 = Offset(b.x + (c.x - b.x) / acLen * round, b.y + (c.y - b.y) / acLen * round)
    val c1 = Offset(c.x - (b.x - c.x) / acLen * round, c.y - (b.y - c.y) / acLen * round)
    val c2 = Offset(c.x + (a.x - c.x) / acLen * round, c.y + (a.y - c.y) / acLen * round)
    val a1 = Offset(baX, a.y)
    return Path().apply {
        moveTo(a1.x, a1.y)
        lineTo(abX, b.y)
        quadraticBezierTo(b.x, b.y, b2.x, b2.y)
        lineTo(c1.x, c1.y)
        quadraticBezierTo(c.x, c.y, c2.x, c2.y)
        lineTo(a2.x, a2.y)
        quadraticBezierTo(a.x, a.y, a1.x, a1.y)
        close()
    }
}

private fun heartPath(cx: Float, topY: Float, u: Float): Path {
    val t = topY
    return Path().apply {
        moveTo(cx, t + 0.032f * u)
        cubicTo(
            cx - 0.030f * u, t + 0.008f * u,
            cx - 0.028f * u, t - 0.014f * u,
            cx - 0.012f * u, t - 0.013f * u,
        )
        cubicTo(
            cx - 0.005f * u, t - 0.012f * u,
            cx, t - 0.004f * u,
            cx, t + 0.002f * u,
        )
        cubicTo(
            cx, t - 0.004f * u,
            cx + 0.005f * u, t - 0.012f * u,
            cx + 0.012f * u, t - 0.013f * u,
        )
        cubicTo(
            cx + 0.028f * u, t - 0.014f * u,
            cx + 0.030f * u, t + 0.008f * u,
            cx, t + 0.032f * u,
        )
        close()
    }
}

private fun catTailPath(cx: Float, u: Float, h: Float): Path =
    Path().apply {
        moveTo(cx + 0.155f * u, h * 0.630f)
        cubicTo(
            cx + 0.380f * u, h * 0.660f,
            cx + 0.470f * u, h * 0.500f,
            cx + 0.420f * u, h * 0.375f,
        )
        quadraticBezierTo(
            cx + 0.400f * u, h * 0.335f,
            cx + 0.345f * u, h * 0.360f,
        )
        cubicTo(
            cx + 0.390f * u, h * 0.450f,
            cx + 0.360f * u, h * 0.520f,
            cx + 0.175f * u, h * 0.585f,
        )
        close()
    }

private fun catEarPath(cx: Float, headCy: Float, u: Float, side: Float): Path =
    Path().apply {
        moveTo(cx + side * 0.24f * u, headCy - 0.20f * u)
        cubicTo(
            cx + side * 0.31f * u, headCy - 0.26f * u,
            cx + side * 0.30f * u, headCy - 0.36f * u,
            cx + side * 0.19f * u, headCy - 0.34f * u,
        )
        cubicTo(
            cx + side * 0.13f * u, headCy - 0.32f * u,
            cx + side * 0.06f * u, headCy - 0.28f * u,
            cx + side * 0.05f * u, headCy - 0.22f * u,
        )
        close()
    }

private fun catInnerEarPath(cx: Float, headCy: Float, u: Float, side: Float): Path =
    Path().apply {
        moveTo(cx + side * 0.175f * u, headCy - 0.245f * u)
        cubicTo(
            cx + side * 0.225f * u, headCy - 0.285f * u,
            cx + side * 0.220f * u, headCy - 0.325f * u,
            cx + side * 0.170f * u, headCy - 0.315f * u,
        )
        cubicTo(
            cx + side * 0.130f * u, headCy - 0.300f * u,
            cx + side * 0.090f * u, headCy - 0.270f * u,
            cx + side * 0.090f * u, headCy - 0.245f * u,
        )
        close()
    }

private fun dogTailPath(cx: Float, u: Float, h: Float): Path =
    Path().apply {
        moveTo(cx + 0.155f * u, h * 0.615f)
        cubicTo(
            cx + 0.340f * u, h * 0.630f,
            cx + 0.430f * u, h * 0.470f,
            cx + 0.355f * u, h * 0.425f,
        )
        quadraticBezierTo(
            cx + 0.385f * u, h * 0.405f,
            cx + 0.368f * u, h * 0.385f,
        )
        cubicTo(
            cx + 0.350f * u, h * 0.470f,
            cx + 0.260f * u, h * 0.560f,
            cx + 0.175f * u, h * 0.565f,
        )
        close()
    }

private fun dogEarPath(cx: Float, headCy: Float, u: Float, side: Float): Path =
    Path().apply {
        moveTo(cx + side * 0.21f * u, headCy - 0.13f * u)
        cubicTo(
            cx + side * 0.40f * u, headCy - 0.02f * u,
            cx + side * 0.42f * u, headCy + 0.20f * u,
            cx + side * 0.335f * u, headCy + 0.33f * u,
        )
        cubicTo(
            cx + side * 0.24f * u, headCy + 0.26f * u,
            cx + side * 0.205f * u, headCy + 0.06f * u,
            cx + side * 0.21f * u, headCy - 0.13f * u,
        )
        close()
    }

private fun dogInnerEarPath(cx: Float, headCy: Float, u: Float, side: Float): Path =
    Path().apply {
        moveTo(cx + side * 0.225f * u, headCy - 0.05f * u)
        cubicTo(
            cx + side * 0.35f * u, headCy + 0.04f * u,
            cx + side * 0.365f * u, headCy + 0.19f * u,
            cx + side * 0.305f * u, headCy + 0.28f * u,
        )
        cubicTo(
            cx + side * 0.255f * u, headCy + 0.22f * u,
            cx + side * 0.235f * u, headCy + 0.05f * u,
            cx + side * 0.225f * u, headCy - 0.05f * u,
        )
        close()
    }