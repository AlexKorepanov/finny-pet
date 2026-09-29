package ru.finny.petgame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import ru.finny.petgame.R
import ru.finny.petgame.ui.model.PetLook
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val PNG_W = 420f
private const val PNG_H = 445f

private val INK = Color(0xFF6C4B22)
private val EYE_DARK = Color(0xFF744A24)
private val PUPIL = Color(0xFF3A2412)
private val NOSE = Color(0xFF714B24)
private val MOUTH_IN = Color(0xFF5B341C)
private val TONGUE = Color(0xFFFF7F8E)
private val TONGUE_DARK = Color(0xFFE0566A)
private val BLUSH = Color(0xFFFF8A80)

private val EYE_COLORS = listOf(
    EYE_DARK,
    Color(0xFF4FA3F0),
    Color(0xFF5DBB63),
    Color(0xFFB0773A),
)

// Координаты черт мордочки в пикселях pet_fennec.png (420×445)
private const val EYE_LX = 155.3f
private const val EYE_RX = 264f
private const val EYE_Y = 221f
private const val EYE_R = 15.5f
private const val FACE_CX = 210.5f

@Composable
fun PetSprite(
    look: PetLook = PetLook.Default,
    modifier: Modifier = Modifier,
    stage: Int = 0,
    eyesClosed: Boolean = false,
) {
    val scale = when (stage.coerceIn(0, 2)) {
        0 -> 0.78f
        1 -> 0.90f
        else -> 1f
    }
    val drawsMouth = look.emotion != 0
    val drawsEyes = drawsMouth || look.eyeColor != 0 || eyesClosed
    val base = when {
        drawsMouth -> R.drawable.pet_fennec_blank
        drawsEyes -> R.drawable.pet_fennec_noeyes
        else -> R.drawable.pet_fennec
    }
    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        Box(modifier = Modifier.fillMaxSize(fraction = scale)) {
            Image(
                painter = painterResource(base),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val s = minOf(size.width / PNG_W, size.height / PNG_H)
                val sp = PngSpace(
                    ox = (size.width - PNG_W * s) / 2f,
                    oy = (size.height - PNG_H * s) / 2f,
                    s = s,
                )
                drawPetAccessories(sp, look, drawsEyes, drawsMouth, eyesClosed)
            }
        }
    }
}

/** Перевод координат PNG в координаты Canvas (как у ContentScale.Fit + Center). */
private class PngSpace(val ox: Float, val oy: Float, val s: Float) {
    fun x(v: Float) = ox + v * s
    fun y(v: Float) = oy + v * s
    fun o(x: Float, y: Float) = Offset(x(x), y(y))
    fun d(v: Float) = v * s
    fun sz(w: Float, h: Float) = Size(w * s, h * s)
}

private class PathBuilder(private val sp: PngSpace) {
    val path = Path()
    fun m(x: Float, y: Float) = path.moveTo(sp.x(x), sp.y(y))
    fun l(x: Float, y: Float) = path.lineTo(sp.x(x), sp.y(y))
    fun q(x1: Float, y1: Float, x2: Float, y2: Float) =
        path.quadraticBezierTo(sp.x(x1), sp.y(y1), sp.x(x2), sp.y(y2))

    fun c(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
        path.cubicTo(sp.x(x1), sp.y(y1), sp.x(x2), sp.y(y2), sp.x(x3), sp.y(y3))

    fun z() = path.close()
}

private fun PngSpace.path(block: PathBuilder.() -> Unit): Path = PathBuilder(this).apply(block).path

private fun PngSpace.stroke(w: Float) = Stroke(width = d(w), cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.drawPetAccessories(
    sp: PngSpace,
    look: PetLook,
    drawsEyes: Boolean,
    drawsMouth: Boolean,
    eyesClosed: Boolean,
) {
    when (look.outfit) {
        1 -> drawScarf(sp)
        2 -> drawHoodie(sp)
        3 -> drawBowTie(sp)
        PetLook.FESTIVE_OUTFIT -> drawFestiveCoat(sp)
    }
    if (drawsEyes) {
        drawFace(sp, look.emotion, look.eyeColor.coerceIn(EYE_COLORS.indices), drawsMouth, eyesClosed)
    }
    when (look.face) {
        1 -> drawRoundGlasses(sp)
        2 -> drawAviators(sp)
        3 -> drawMonocle(sp)
    }
    when (look.hat) {
        1 -> drawCap(sp)
        2 -> drawBow(sp)
        3 -> drawPartyHat(sp)
    }
}

// ---------- Мордочка ----------

private fun DrawScope.drawFace(sp: PngSpace, emotion: Int, eyeColor: Int, drawsMouth: Boolean, eyesClosed: Boolean) {
    if (eyesClosed && emotion != 1) {
        drawFaceBlinking(sp, emotion, drawsMouth)
        return
    }
    when (emotion) {
        1 -> {
            drawBlush(sp, both = true)
            drawHappyEye(sp, EYE_LX)
            drawHappyEye(sp, EYE_RX)
            drawNose(sp)
            drawGentleSmile(sp)
        }
        2 -> {
            drawBlush(sp, both = true)
            drawOpenEye(sp, EYE_LX, EYE_Y, EYE_R, eyeColor, sparkle = false)
            drawWinkEye(sp, EYE_RX)
            drawNose(sp)
            drawTongueSmile(sp)
        }
        3 -> {
            drawBlush(sp, both = true, strong = true)
            drawOpenEye(sp, EYE_LX, EYE_Y - 1f, EYE_R * 1.3f, eyeColor, sparkle = true)
            drawOpenEye(sp, EYE_RX, EYE_Y - 1f, EYE_R * 1.3f, eyeColor, sparkle = true)
            drawNose(sp)
            drawBigOpenMouth(sp)
        }
        else -> {
            drawOpenEye(sp, EYE_LX, EYE_Y, EYE_R, eyeColor, sparkle = false)
            drawOpenEye(sp, EYE_RX, EYE_Y, EYE_R, eyeColor, sparkle = false)
            if (drawsMouth) {
                drawNose(sp)
                drawOpenSmile(sp)
            }
        }
    }
}

/** Та же мордочка, но оба глаза на миг закрыты. */
private fun DrawScope.drawFaceBlinking(sp: PngSpace, emotion: Int, drawsMouth: Boolean) {
    if (emotion == 2 || emotion == 3) drawBlush(sp, both = true, strong = emotion == 3)
    drawClosedEye(sp, EYE_LX)
    drawClosedEye(sp, EYE_RX)
    if (!drawsMouth) return
    drawNose(sp)
    when (emotion) {
        2 -> drawTongueSmile(sp)
        3 -> drawBigOpenMouth(sp)
        else -> drawOpenSmile(sp)
    }
}

/** Закрытый глаз «◡» при моргании. */
private fun DrawScope.drawClosedEye(sp: PngSpace, cx: Float) {
    drawPath(
        sp.path {
            m(cx - 14f, EYE_Y - 1f)
            q(cx, EYE_Y + 11f, cx + 14f, EYE_Y - 1f)
        },
        color = EYE_DARK,
        style = sp.stroke(7f),
    )
}

private fun DrawScope.drawOpenEye(sp: PngSpace, cx: Float, cy: Float, r: Float, colorIndex: Int, sparkle: Boolean) {
    val c = sp.o(cx, cy)
    drawCircle(color = EYE_DARK, radius = sp.d(r), center = c)
    if (colorIndex != 0) {
        drawCircle(color = EYE_COLORS[colorIndex], radius = sp.d(r * 0.78f), center = c)
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent),
                startY = sp.y(cy - r),
                endY = sp.y(cy),
            ),
            radius = sp.d(r * 0.78f),
            center = c,
        )
        drawCircle(color = PUPIL, radius = sp.d(r * 0.42f), center = sp.o(cx, cy + r * 0.05f))
    }
    if (sparkle) {
        drawPath(
            starPath(sp, cx - r * 0.30f, cy - r * 0.32f, r * 0.50f, r * 0.14f, points = 4),
            color = Color.White,
        )
        drawCircle(color = Color.White, radius = sp.d(r * 0.16f), center = sp.o(cx + r * 0.38f, cy + r * 0.32f))
        drawCircle(
            color = Color.White.copy(alpha = 0.7f),
            radius = sp.d(r * 0.08f),
            center = sp.o(cx + r * 0.05f, cy + r * 0.52f),
        )
    } else {
        drawCircle(color = Color.White, radius = sp.d(r * 0.30f), center = sp.o(cx - r * 0.32f, cy - r * 0.34f))
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = sp.d(r * 0.13f),
            center = sp.o(cx + r * 0.36f, cy + r * 0.30f),
        )
    }
}

/** Закрытый счастливый глаз «^». */
private fun DrawScope.drawHappyEye(sp: PngSpace, cx: Float) {
    drawPath(
        sp.path {
            m(cx - 15f, EYE_Y + 5f)
            q(cx, EYE_Y - 17f, cx + 15f, EYE_Y + 5f)
        },
        color = EYE_DARK,
        style = sp.stroke(7.5f),
    )
}

private fun DrawScope.drawWinkEye(sp: PngSpace, cx: Float) {
    drawPath(
        sp.path {
            m(cx - 16f, EYE_Y + 2f)
            q(cx, EYE_Y - 12f, cx + 16f, EYE_Y + 2f)
        },
        color = EYE_DARK,
        style = sp.stroke(7.5f),
    )
    drawLine(
        color = EYE_DARK,
        start = sp.o(cx + 14f, EYE_Y),
        end = sp.o(cx + 22f, EYE_Y - 6f),
        strokeWidth = sp.d(4.5f),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = EYE_DARK,
        start = sp.o(cx + 15f, EYE_Y + 3f),
        end = sp.o(cx + 24f, EYE_Y + 2f),
        strokeWidth = sp.d(4.5f),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawBlush(sp: PngSpace, both: Boolean, strong: Boolean = false) {
    val alpha = if (strong) 0.55f else 0.42f
    val xs = if (both) listOf(120f, 301f) else listOf(301f)
    xs.forEach { x ->
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(BLUSH.copy(alpha = alpha), BLUSH.copy(alpha = 0f)),
                center = sp.o(x, 254f),
                radius = sp.d(22f),
            ),
            topLeft = sp.o(x - 22f, 242f),
            size = sp.sz(44f, 24f),
        )
    }
}

private fun DrawScope.drawNose(sp: PngSpace) {
    val nose = sp.path {
        m(211f, 235f)
        c(221f, 235f, 228f, 238f, 227f, 244f)
        c(226f, 249f, 217f, 254f, 211f, 254f)
        c(205f, 254f, 196f, 249f, 195f, 244f)
        c(194f, 238f, 201f, 235f, 211f, 235f)
        z()
    }
    drawPath(nose, color = NOSE)
    drawOval(
        color = Color.White.copy(alpha = 0.45f),
        topLeft = sp.o(201f, 238f),
        size = sp.sz(9f, 5f),
    )
}

private fun DrawScope.drawNoseStem(sp: PngSpace, toY: Float) {
    drawLine(
        color = INK,
        start = sp.o(FACE_CX, 252f),
        end = sp.o(FACE_CX, toY),
        strokeWidth = sp.d(6f),
        cap = StrokeCap.Round,
    )
}

/** «Радость» с цветными глазами — как оригинал: открытая улыбка. */
private fun DrawScope.drawOpenSmile(sp: PngSpace) {
    drawNoseStem(sp, 260f)
    val mouth = sp.path {
        m(189f, 259f)
        q(FACE_CX, 266f, 232f, 259f)
        c(231f, 271f, 221f, 275f, FACE_CX, 275f)
        c(200f, 275f, 190f, 271f, 189f, 259f)
        z()
    }
    drawPath(mouth, color = MOUTH_IN)
    clipPath(mouth) {
        drawCircle(color = TONGUE, radius = sp.d(10f), center = sp.o(FACE_CX, 279f))
    }
    drawPath(mouth, color = INK, style = sp.stroke(4.5f))
}

/** «Улыбка» — мягкая кошачья «ω». */
private fun DrawScope.drawGentleSmile(sp: PngSpace) {
    drawNoseStem(sp, 259f)
    drawPath(
        sp.path {
            m(188f, 255f)
            q(198f, 270f, FACE_CX, 259f)
            q(223f, 270f, 233f, 255f)
        },
        color = INK,
        style = sp.stroke(6.5f),
    )
}

/** «Подмигивание» — улыбка с высунутым кончиком языка. */
private fun DrawScope.drawTongueSmile(sp: PngSpace) {
    drawNoseStem(sp, 259f)
    val tongue = sp.path {
        m(213f, 263f)
        c(212f, 281f, 233f, 283f, 232f, 263f)
        z()
    }
    drawPath(tongue, color = TONGUE)
    drawLine(
        color = TONGUE_DARK,
        start = sp.o(222.5f, 266f),
        end = sp.o(222.5f, 274f),
        strokeWidth = sp.d(2.5f),
        cap = StrokeCap.Round,
    )
    drawPath(tongue, color = INK, style = sp.stroke(4f))
    drawPath(
        sp.path {
            m(188f, 256f)
            q(198f, 270f, FACE_CX, 259f)
            q(223f, 270f, 234f, 255f)
        },
        color = INK,
        style = sp.stroke(6.5f),
    )
}

/** «Восторг» — большой открытый рот с языком. */
private fun DrawScope.drawBigOpenMouth(sp: PngSpace) {
    drawNoseStem(sp, 258f)
    val mouth = sp.path {
        m(185f, 257f)
        q(FACE_CX, 265f, 236f, 257f)
        c(236f, 274f, 225f, 281f, FACE_CX, 281f)
        c(196f, 281f, 185f, 274f, 185f, 257f)
        z()
    }
    drawPath(mouth, color = MOUTH_IN)
    clipPath(mouth) {
        drawOval(color = TONGUE, topLeft = sp.o(193f, 268f), size = sp.sz(35f, 22f))
        drawLine(
            color = TONGUE_DARK,
            start = sp.o(FACE_CX, 271f),
            end = sp.o(FACE_CX, 281f),
            strokeWidth = sp.d(2.5f),
            cap = StrokeCap.Round,
        )
    }
    drawPath(mouth, color = INK, style = sp.stroke(5f))
}

private fun starPath(sp: PngSpace, cx: Float, cy: Float, rOuter: Float, rInner: Float, points: Int): Path {
    val path = Path()
    val steps = points * 2
    for (i in 0 until steps) {
        val r = if (i % 2 == 0) rOuter else rInner
        val a = Math.PI * i / points - Math.PI / 2
        val x = sp.x(cx + (r * cos(a)).toFloat())
        val y = sp.y(cy + (r * sin(a)).toFloat())
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

// ---------- Очки ----------

private fun DrawScope.drawRoundGlasses(sp: PngSpace) {
    val frame = Color(0xFF37474F)
    val r = 26f
    listOf(EYE_LX, EYE_RX).forEach { x ->
        drawCircle(color = Color(0xFF81D4FA).copy(alpha = 0.25f), radius = sp.d(r), center = sp.o(x, EYE_Y))
        drawArc(
            color = Color.White.copy(alpha = 0.55f),
            startAngle = 200f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = sp.o(x - r + 7f, EYE_Y - r + 7f),
            size = sp.sz((r - 7f) * 2f, (r - 7f) * 2f),
            style = sp.stroke(4f),
        )
        drawCircle(color = frame, radius = sp.d(r), center = sp.o(x, EYE_Y), style = Stroke(width = sp.d(6f)))
    }
    drawPath(
        sp.path {
            m(EYE_LX + r, EYE_Y - 4f)
            q(FACE_CX, EYE_Y - 14f, EYE_RX - r, EYE_Y - 4f)
        },
        color = frame,
        style = sp.stroke(5.5f),
    )
    drawLine(frame, sp.o(EYE_LX - r, EYE_Y - 6f), sp.o(97f, EYE_Y - 13f), sp.d(5f), StrokeCap.Round)
    drawLine(frame, sp.o(EYE_RX + r, EYE_Y - 6f), sp.o(323f, EYE_Y - 13f), sp.d(5f), StrokeCap.Round)
}

/** Ray-Ban Aviator: плоский верх, капля к носу, двойной мост. */
private fun aviatorLens(sp: PngSpace, ex: Float, side: Float): Path {
    val ey = EYE_Y
    fun px(dx: Float) = ex + side * dx
    return sp.path {
        m(px(-24f), ey - 22f)
        c(px(-6f), ey - 26f, px(20f), ey - 26f, px(32f), ey - 21f)
        c(px(40f), ey - 17f, px(38f), ey + 2f, px(30f), ey + 12f)
        c(px(22f), ey + 24f, px(8f), ey + 30f, px(-6f), ey + 30f)
        c(px(-18f), ey + 30f, px(-26f), ey + 22f, px(-28f), ey + 10f)
        c(px(-30f), ey - 2f, px(-30f), ey - 18f, px(-24f), ey - 22f)
        z()
    }
}

private fun DrawScope.drawAviators(sp: PngSpace) {
    val gold = Color(0xFFE2BE55)
    val goldDark = Color(0xFF8D6E16)
    val lenses = listOf(EYE_LX to -1f, EYE_RX to 1f).map { (x, side) -> aviatorLens(sp, x, side) }
    lenses.forEach { lens ->
        drawPath(
            lens,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF6D4C41), Color(0xFF3E2723), Color(0xFF1A0F0A)),
                startY = sp.y(EYE_Y - 24f),
                endY = sp.y(EYE_Y + 30f),
            ),
            alpha = 0.96f,
        )
        clipPath(lens) {
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = sp.o(100f, EYE_Y + 10f),
                end = sp.o(320f, EYE_Y - 40f),
                strokeWidth = sp.d(9f),
            )
        }
    }
    // Мосты: верхняя планка и нижний мостик над носом
    drawLine(goldDark, sp.o(EYE_LX + 22f, EYE_Y - 21f), sp.o(EYE_RX - 22f, EYE_Y - 21f), sp.d(6f), StrokeCap.Round)
    drawLine(gold, sp.o(EYE_LX + 22f, EYE_Y - 21f), sp.o(EYE_RX - 22f, EYE_Y - 21f), sp.d(3f), StrokeCap.Round)
    val bridge = sp.path {
        m(EYE_LX + 29f, EYE_Y - 9f)
        q(FACE_CX, EYE_Y - 20f, EYE_RX - 29f, EYE_Y - 9f)
    }
    drawPath(bridge, color = goldDark, style = sp.stroke(5.5f))
    drawPath(bridge, color = gold, style = sp.stroke(2.8f))
    lenses.forEach { lens ->
        drawPath(lens, color = goldDark, style = sp.stroke(5.5f))
        drawPath(lens, color = gold, style = sp.stroke(2.8f))
    }
    listOf(EYE_LX to -1f, EYE_RX to 1f).forEach { (x, side) ->
        val start = sp.o(x + side * 34f, EYE_Y - 17f)
        val end = sp.o(if (side < 0) 96f else 324f, EYE_Y - 14f)
        drawLine(goldDark, start, end, sp.d(5f), StrokeCap.Round)
        drawLine(gold, start, end, sp.d(2.5f), StrokeCap.Round)
    }
}

private fun DrawScope.drawMonocle(sp: PngSpace) {
    val gold = Color(0xFFFFC233)
    val goldDark = Color(0xFFB8860B)
    val r = 27f
    val c = sp.o(EYE_RX, EYE_Y)
    drawCircle(color = Color(0xFFFFF59D).copy(alpha = 0.22f), radius = sp.d(r), center = c)
    drawArc(
        color = Color.White.copy(alpha = 0.6f),
        startAngle = 200f,
        sweepAngle = 60f,
        useCenter = false,
        topLeft = sp.o(EYE_RX - r + 7f, EYE_Y - r + 7f),
        size = sp.sz((r - 7f) * 2f, (r - 7f) * 2f),
        style = sp.stroke(4f),
    )
    drawCircle(color = goldDark, radius = sp.d(r), center = c, style = Stroke(width = sp.d(8f)))
    drawCircle(color = gold, radius = sp.d(r), center = c, style = Stroke(width = sp.d(4.5f)))
    val chain = sp.path {
        m(EYE_RX + 17f, EYE_Y + 22f)
        c(300f, 262f, 298f, 292f, 272f, 303f)
        c(258f, 309f, 250f, 316f, 252f, 328f)
    }
    drawPath(
        chain,
        color = goldDark,
        style = Stroke(
            width = sp.d(3.5f),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(sp.d(4f), sp.d(3.5f))),
        ),
    )
    drawCircle(color = gold, radius = sp.d(5f), center = sp.o(252f, 330f))
    drawCircle(color = goldDark, radius = sp.d(5f), center = sp.o(252f, 330f), style = Stroke(width = sp.d(2f)))
}

// ---------- Головные уборы ----------

private fun DrawScope.drawCap(sp: PngSpace) {
    val blueLight = Color(0xFF4FC3F7)
    val blue = Color(0xFF0288D1)
    val blueDark = Color(0xFF01579B)
    val crown = sp.path {
        m(142f, 142f)
        c(142f, 98f, 170f, 76f, 211f, 76f)
        c(252f, 76f, 280f, 98f, 280f, 142f)
        q(211f, 154f, 142f, 142f)
        z()
    }
    drawPath(
        crown,
        brush = Brush.verticalGradient(
            colors = listOf(blueLight, blue),
            startY = sp.y(76f),
            endY = sp.y(150f),
        ),
    )
    clipPath(crown) {
        drawPath(
            sp.path {
                m(208f, 80f)
                q(180f, 100f, 176f, 150f)
            },
            color = blueDark.copy(alpha = 0.45f),
            style = sp.stroke(3f),
        )
        drawPath(
            sp.path {
                m(214f, 80f)
                q(242f, 100f, 246f, 150f)
            },
            color = blueDark.copy(alpha = 0.45f),
            style = sp.stroke(3f),
        )
        drawOval(
            color = blueDark.copy(alpha = 0.25f),
            topLeft = sp.o(252f, 80f),
            size = sp.sz(50f, 80f),
        )
    }
    drawPath(
        sp.path {
            m(156f, 128f)
            q(158f, 100f, 184f, 88f)
        },
        color = Color.White.copy(alpha = 0.45f),
        style = sp.stroke(5f),
    )
    drawPath(crown, color = INK, style = sp.stroke(6f))
    // Значок
    drawCircle(color = Color(0xFFFFD54F), radius = sp.d(14f), center = sp.o(211f, 116f))
    drawPath(starPath(sp, 211f, 117f, 9f, 4f, points = 5), color = Color(0xFFFF8F00))
    drawCircle(color = INK, radius = sp.d(14f), center = sp.o(211f, 116f), style = Stroke(width = sp.d(3.5f)))
    // Пуговка
    drawCircle(color = blue, radius = sp.d(7f), center = sp.o(211f, 77f))
    drawCircle(color = INK, radius = sp.d(7f), center = sp.o(211f, 77f), style = Stroke(width = sp.d(4f)))

    // Тень от козырька на лбу
    drawOval(
        color = Color.Black.copy(alpha = 0.10f),
        topLeft = sp.o(156f, 168f),
        size = sp.sz(110f, 18f),
    )
    val brim = sp.path {
        m(138f, 141f)
        q(211f, 158f, 284f, 141f)
        q(294f, 160f, 264f, 171f)
        q(211f, 186f, 158f, 171f)
        q(128f, 160f, 138f, 141f)
        z()
    }
    drawPath(
        brim,
        brush = Brush.verticalGradient(
            colors = listOf(blue, blueDark),
            startY = sp.y(141f),
            endY = sp.y(182f),
        ),
    )
    drawPath(
        sp.path {
            m(160f, 158f)
            q(211f, 170f, 262f, 158f)
        },
        color = Color.White.copy(alpha = 0.25f),
        style = sp.stroke(4f),
    )
    drawPath(brim, color = INK, style = sp.stroke(6f))
}

private fun DrawScope.drawBow(sp: PngSpace) {
    val pink = Color(0xFFFF80AB)
    val pinkLight = Color(0xFFFFB3CF)
    val pinkDark = Color(0xFFD81B60)
    val bx = 268f
    val by = 124f
    // Хвостики ленты
    listOf(-1f, 1f).forEach { side ->
        val tail = sp.path {
            m(bx + side * 3f, by + 4f)
            l(bx + side * 14f, by + 34f)
            l(bx + side * 7f, by + 30f)
            l(bx + side * 2f, by + 36f)
            l(bx - side * 3f, by + 6f)
            z()
        }
        drawPath(tail, color = pinkDark)
        drawPath(tail, color = INK, style = sp.stroke(3.5f))
    }
    listOf(-1f, 1f).forEach { side ->
        val wing = sp.path {
            m(bx, by)
            c(bx + side * 18f, by - 30f, bx + side * 42f, by - 22f, bx + side * 38f, by)
            c(bx + side * 42f, by + 22f, bx + side * 18f, by + 26f, bx, by)
            z()
        }
        drawPath(
            wing,
            brush = Brush.verticalGradient(
                colors = listOf(pinkLight, pink),
                startY = sp.y(by - 26f),
                endY = sp.y(by + 22f),
            ),
        )
        drawPath(
            sp.path {
                m(bx + side * 10f, by - 4f)
                q(bx + side * 22f, by - 10f, bx + side * 30f, by - 6f)
            },
            color = pinkDark.copy(alpha = 0.5f),
            style = sp.stroke(3f),
        )
        drawPath(
            sp.path {
                m(bx + side * 10f, by + 4f)
                q(bx + side * 22f, by + 10f, bx + side * 30f, by + 7f)
            },
            color = pinkDark.copy(alpha = 0.5f),
            style = sp.stroke(3f),
        )
        drawPath(wing, color = INK, style = sp.stroke(4.5f))
    }
    drawRoundRect(
        color = pink,
        topLeft = sp.o(bx - 9f, by - 11f),
        size = sp.sz(18f, 22f),
        cornerRadius = CornerRadius(sp.d(7f), sp.d(7f)),
    )
    drawRoundRect(
        color = INK,
        topLeft = sp.o(bx - 9f, by - 11f),
        size = sp.sz(18f, 22f),
        cornerRadius = CornerRadius(sp.d(7f), sp.d(7f)),
        style = Stroke(width = sp.d(4f)),
    )
    drawCircle(color = Color.White.copy(alpha = 0.6f), radius = sp.d(3f), center = sp.o(bx - 3f, by - 5f))
}

private fun DrawScope.drawPartyHat(sp: PngSpace) {
    val tip = Offset(205f, 26f)
    val hat = sp.path {
        m(tip.x, tip.y)
        l(254f, 121f)
        q(211f, 133f, 166f, 121f)
        z()
    }
    drawOval(
        color = Color.Black.copy(alpha = 0.12f),
        topLeft = sp.o(166f, 118f),
        size = sp.sz(88f, 16f),
    )
    drawPath(
        hat,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFF8A65), Color(0xFFFF5252), Color(0xFF7C4DFF)),
            startY = sp.y(tip.y),
            endY = sp.y(128f),
        ),
    )
    clipPath(hat) {
        listOf(52f, 78f, 104f).forEach { y ->
            drawPath(
                sp.path {
                    m(150f, y + 4f)
                    q(211f, y + 14f, 270f, y + 4f)
                },
                color = Color.White.copy(alpha = 0.55f),
                style = Stroke(width = sp.d(7f)),
            )
        }
        listOf(186f to 92f, 224f to 70f, 214f to 112f).forEach { (x, y) ->
            drawCircle(color = Color(0xFFFFEB3B), radius = sp.d(3.5f), center = sp.o(x, y))
        }
    }
    drawPath(hat, color = INK, style = sp.stroke(6f))
    drawPath(
        sp.path {
            m(168f, 121f)
            q(211f, 133f, 252f, 121f)
        },
        color = Color(0xFFFFD54F),
        style = sp.stroke(7f),
    )
    drawCircle(color = Color(0xFFFFF176), radius = sp.d(13f), center = sp.o(tip.x, tip.y))
    drawCircle(color = Color.White.copy(alpha = 0.7f), radius = sp.d(4f), center = sp.o(tip.x - 4f, tip.y - 4f))
    drawCircle(color = INK, radius = sp.d(13f), center = sp.o(tip.x, tip.y), style = Stroke(width = sp.d(4.5f)))
}

// ---------- Одежда ----------

private fun DrawScope.drawScarf(sp: PngSpace) {
    val red = Color(0xFFEF5350)
    val redDark = Color(0xFFC62828)
    val stripe = Color(0xFFFFF3E0)
    val end = sp.path {
        m(236f, 306f)
        c(248f, 330f, 258f, 350f, 257f, 380f)
        l(227f, 382f)
        c(229f, 356f, 224f, 334f, 214f, 314f)
        z()
    }
    drawPath(end, brush = Brush.horizontalGradient(listOf(red, redDark), startX = sp.x(214f), endX = sp.x(258f)))
    clipPath(end) {
        drawLine(stripe, sp.o(200f, 352f), sp.o(270f, 350f), sp.d(6f))
        drawLine(stripe, sp.o(200f, 366f), sp.o(270f, 364f), sp.d(6f))
    }
    drawPath(end, color = INK, style = sp.stroke(5f))
    listOf(231f, 238f, 245f, 252f).forEach { x ->
        drawLine(red, sp.o(x, 383f), sp.o(x + 1f, 394f), sp.d(4f), StrokeCap.Round)
    }

    val wrap = sp.path {
        m(140f, 286f)
        q(211f, 300f, 282f, 286f)
        c(293f, 290f, 292f, 311f, 279f, 314f)
        q(211f, 330f, 143f, 314f)
        c(130f, 311f, 129f, 290f, 140f, 286f)
        z()
    }
    drawPath(
        wrap,
        brush = Brush.verticalGradient(listOf(Color(0xFFFF7A76), red, redDark), startY = sp.y(286f), endY = sp.y(324f)),
    )
    clipPath(wrap) {
        drawPath(
            sp.path {
                m(130f, 299f)
                q(211f, 316f, 292f, 299f)
            },
            color = stripe,
            style = Stroke(width = sp.d(6f)),
        )
        for (x in 150..270 step 12) {
            drawLine(
                color = redDark.copy(alpha = 0.35f),
                start = sp.o(x.toFloat(), 288f),
                end = sp.o(x.toFloat() + 3f, 326f),
                strokeWidth = sp.d(2f),
            )
        }
    }
    drawPath(wrap, color = INK, style = sp.stroke(5.5f))
    // Узелок
    drawOval(color = red, topLeft = sp.o(222f, 298f), size = sp.sz(26f, 22f))
    drawOval(color = INK, topLeft = sp.o(222f, 298f), size = sp.sz(26f, 22f), style = Stroke(width = sp.d(4.5f)))
}

private fun DrawScope.drawHoodie(sp: PngSpace) {
    val light = Color(0xFFB39DDB)
    val main = Color(0xFF9575CD)
    val dark = Color(0xFF7E57C2)
    val deep = Color(0xFF5E35B1)
    val cord = Color(0xFFFFF8E1)

    // Торс по контуру тела: шея ~y290, бока x≈146…284, подол над лапками
    val torso = sp.path {
        m(150f, 289f)
        q(211f, 299f, 272f, 289f)
        c(281f, 300f, 286f, 318f, 285f, 336f)
        l(284f, 364f)
        q(211f, 376f, 146f, 364f)
        l(144f, 336f)
        c(143f, 318f, 142f, 301f, 150f, 289f)
        z()
    }
    drawPath(
        torso,
        brush = Brush.verticalGradient(listOf(light, main), startY = sp.y(290f), endY = sp.y(370f)),
    )
    clipPath(torso) {
        // Объём по бокам
        drawOval(color = dark.copy(alpha = 0.55f), topLeft = sp.o(258f, 290f), size = sp.sz(50f, 90f))
        drawOval(color = dark.copy(alpha = 0.35f), topLeft = sp.o(124f, 290f), size = sp.sz(38f, 90f))
        // Рукава поверх передних лап
        listOf(
            sp.path { m(170f, 298f); q(184f, 326f, 191f, 372f) },
            sp.path { m(252f, 298f); q(238f, 326f, 232f, 372f) },
        ).forEach { seam ->
            drawPath(seam, color = deep.copy(alpha = 0.55f), style = sp.stroke(3.5f))
        }
        // Резинка по низу
        drawPath(
            sp.path {
                m(130f, 352f)
                q(211f, 364f, 300f, 352f)
                l(300f, 380f)
                l(130f, 380f)
                z()
            },
            color = dark,
        )
        for (x in 150..282 step 9) {
            drawLine(
                color = deep.copy(alpha = 0.6f),
                start = sp.o(x.toFloat(), 356f),
                end = sp.o(x.toFloat(), 374f),
                strokeWidth = sp.d(2f),
            )
        }
    }
    drawPath(torso, color = INK, style = sp.stroke(6f))

    // Карман-кенгуру
    val pocket = sp.path {
        m(186f, 322f)
        q(211f, 318f, 236f, 322f)
        l(244f, 348f)
        q(211f, 354f, 178f, 348f)
        z()
    }
    drawPath(pocket, color = dark)
    drawPath(pocket, color = INK, style = sp.stroke(4f))
    drawPath(sp.path { m(186f, 324f); q(180f, 336f, 181f, 346f) }, color = deep, style = sp.stroke(3f))
    drawPath(sp.path { m(236f, 324f); q(242f, 336f, 241f, 346f) }, color = deep, style = sp.stroke(3f))

    // Капюшон — толстый ворот вокруг шеи
    val hood = sp.path {
        m(140f, 287f)
        q(211f, 303f, 282f, 287f)
        c(293f, 287f, 295f, 302f, 284f, 306f)
        q(211f, 324f, 138f, 306f)
        c(127f, 302f, 129f, 287f, 140f, 287f)
        z()
    }
    drawPath(
        hood,
        brush = Brush.verticalGradient(listOf(dark, deep), startY = sp.y(288f), endY = sp.y(318f)),
    )
    drawPath(
        sp.path {
            m(146f, 294f)
            q(211f, 308f, 276f, 294f)
        },
        color = light.copy(alpha = 0.6f),
        style = sp.stroke(3.5f),
    )
    drawPath(hood, color = INK, style = sp.stroke(5.5f))

    // Шнурки
    listOf(199f to 195f, 223f to 227f).forEach { (x0, x1) ->
        drawLine(cord, sp.o(x0, 312f), sp.o(x1, 338f), sp.d(4f), StrokeCap.Round)
        drawRoundRect(
            color = Color(0xFFFFC107),
            topLeft = sp.o(x1 - 3f, 336f),
            size = sp.sz(6f, 9f),
            cornerRadius = CornerRadius(sp.d(2.5f), sp.d(2.5f)),
        )
    }
    drawCircle(color = INK, radius = sp.d(3f), center = sp.o(199f, 312f))
    drawCircle(color = INK, radius = sp.d(3f), center = sp.o(223f, 312f))
}

/** Праздничный фрак из копилки: сидит по фигуре, золотая кайма, манишка, бабочка и медаль. */
private fun DrawScope.drawFestiveCoat(sp: PngSpace) {
    val redLight = Color(0xFFE8474A)
    val red = Color(0xFFC62834)
    val redDark = Color(0xFF8E1623)
    val seam = Color(0x966E0F1C)
    val goldLight = Color(0xFFFFE68A)
    val gold = Color(0xFFF7C548)
    val goldDark = Color(0xFFC98A1B)
    val shirt = Color(0xFFFFF8EA)
    val cx = FACE_CX

    val jacket = sp.path {
        m(154f, 289f)
        q(cx, 300f, 268f, 289f)
        c(277f, 302f, 281f, 322f, 281f, 342f)
        c(281f, 356f, 279f, 366f, 270f, 370f)
        q(244f, 378f, cx + 4f, 377f)
        l(cx, 371f)
        l(cx - 4f, 377f)
        q(178f, 378f, 152f, 370f)
        c(143f, 366f, 141f, 356f, 141f, 342f)
        c(141f, 322f, 145f, 302f, 154f, 289f)
        z()
    }
    drawPath(
        jacket,
        brush = Brush.verticalGradient(listOf(redLight, red, redDark), startY = sp.y(290f), endY = sp.y(380f)),
    )
    clipPath(jacket) {
        drawPath(
            sp.path { m(262f, 285f); q(296f, 330f, 286f, 380f); l(300f, 380f); l(300f, 285f); z() },
            color = Color(0x465A0A14),
        )
        drawPath(
            sp.path { m(160f, 285f); q(132f, 330f, 138f, 380f); l(120f, 380f); l(120f, 285f); z() },
            color = Color(0x325A0A14),
        )
        // Рукава поверх передних лап
        drawPath(sp.path { m(172f, 300f); q(182f, 330f, 190f, 372f) }, color = seam, style = sp.stroke(3.2f))
        drawPath(sp.path { m(250f, 300f); q(240f, 330f, 232f, 372f) }, color = seam, style = sp.stroke(3.2f))
        // Кайма по подолу
        drawPath(sp.path { m(146f, 364f); c(150f, 372f, 180f, 375f, cx - 4f, 374f) }, color = gold, style = sp.stroke(5f))
        drawPath(sp.path { m(276f, 364f); c(272f, 372f, 242f, 375f, cx + 4f, 374f) }, color = gold, style = sp.stroke(5f))
    }

    val bib = sp.path {
        m(184f, 294f)
        q(cx, 302f, 238f, 294f)
        l(cx, 346f)
        z()
    }
    drawPath(bib, color = shirt)
    clipPath(bib) {
        drawPath(
            sp.path { m(cx + 6f, 300f); l(240f, 294f); l(cx + 2f, 346f); z() },
            color = Color(0x78EADFC8),
        )
    }

    // Лацканы — золотая кайма по краю выреза
    drawPath(sp.path { m(176f, 293f); l(186f, 294f); l(cx, 346f); l(cx - 5f, 352f); z() }, color = gold)
    drawPath(sp.path { m(246f, 293f); l(236f, 294f); l(cx, 346f); l(cx + 5f, 352f); z() }, color = gold)
    drawLine(goldLight, sp.o(181f, 294f), sp.o(cx - 1f, 347f), sp.d(1.6f), StrokeCap.Round)
    drawLine(goldLight, sp.o(241f, 294f), sp.o(cx + 1f, 347f), sp.d(1.6f), StrokeCap.Round)

    drawPath(jacket, color = INK, style = sp.stroke(5.5f))
    drawPath(bib, color = INK, style = sp.stroke(3f))
    drawLine(INK, sp.o(cx, 348f), sp.o(cx, 371f), sp.d(3f), StrokeCap.Round)

    listOf(353f, 364f).forEach { y ->
        drawCircle(color = gold, radius = sp.d(4.2f), center = sp.o(cx + 10f, y))
        drawCircle(color = INK, radius = sp.d(4.2f), center = sp.o(cx + 10f, y), style = Stroke(width = sp.d(2.2f)))
        drawCircle(color = goldLight, radius = sp.d(1.3f), center = sp.o(cx + 8.8f, y - 1.3f))
    }

    // Бабочка
    val by = 302f
    listOf(-1f, 1f).forEach { side ->
        val wing = sp.path {
            m(cx + side * 4f, by)
            c(cx + side * 10f, by - 8f, cx + side * 20f, by - 10f, cx + side * 22f, by - 5f)
            c(cx + side * 24f, by, cx + side * 24f, by + 3f, cx + side * 22f, by + 7f)
            c(cx + side * 20f, by + 11f, cx + side * 10f, by + 8f, cx + side * 4f, by)
            z()
        }
        drawPath(wing, brush = Brush.verticalGradient(listOf(goldLight, gold), startY = sp.y(by - 10f), endY = sp.y(by + 10f)))
        drawPath(wing, color = INK, style = sp.stroke(3f))
    }
    drawRect(color = goldDark, topLeft = sp.o(cx - 5f, by - 6f), size = sp.sz(10f, 12f))
    drawRect(color = INK, topLeft = sp.o(cx - 5f, by - 6f), size = sp.sz(10f, 12f), style = sp.stroke(3f))

    // Медаль на ленточке
    val ribbon = sp.path { m(172f, 314f); l(184f, 314f); l(182f, 320f); l(174f, 320f); z() }
    drawPath(ribbon, color = Color(0xFF2E7FD9))
    drawPath(ribbon, color = INK, style = sp.stroke(2.2f))
    drawCircle(color = gold, radius = sp.d(11f), center = sp.o(178f, 330f))
    drawCircle(color = INK, radius = sp.d(11f), center = sp.o(178f, 330f), style = Stroke(width = sp.d(2.8f)))
    drawPath(sp.star(178f, 330.5f, 7.5f), color = Color.White)
    drawPath(sp.star(178f, 330.5f, 5.8f), color = goldDark)
}

private fun PngSpace.star(cx: Float, cy: Float, outer: Float): Path {
    val path = Path()
    val inner = outer * 0.45f
    for (i in 0 until 10) {
        val angle = -PI / 2.0 + i * PI / 5.0
        val radius = if (i % 2 == 0) outer else inner
        val px = cx + (radius * cos(angle)).toFloat()
        val py = cy + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x(px), y(py)) else path.lineTo(x(px), y(py))
    }
    path.close()
    return path
}

private fun DrawScope.drawBowTie(sp: PngSpace) {
    val teal = Color(0xFF26A69A)
    val tealLight = Color(0xFF4DD0C1)
    val tealDark = Color(0xFF00796B)
    val cx = 211f
    val cy = 304f
    listOf(-1f, 1f).forEach { side ->
        fun px(dx: Float) = cx + side * dx
        val wing = sp.path {
            m(px(5f), cy)
            c(px(15f), cy - 9f, px(28f), cy - 18f, px(33f), cy - 12f)
            c(px(38f), cy - 5f, px(38f), cy + 5f, px(33f), cy + 12f)
            c(px(28f), cy + 18f, px(15f), cy + 9f, px(5f), cy)
            z()
        }
        drawPath(
            wing,
            brush = Brush.verticalGradient(listOf(tealLight, teal), startY = sp.y(cy - 16f), endY = sp.y(cy + 16f)),
        )
        drawPath(
            sp.path {
                m(px(12f), cy - 2f)
                q(px(22f), cy - 6f, px(28f), cy - 6f)
            },
            color = tealDark.copy(alpha = 0.6f),
            style = sp.stroke(2.5f),
        )
        drawPath(
            sp.path {
                m(px(12f), cy + 2f)
                q(px(22f), cy + 6f, px(28f), cy + 7f)
            },
            color = tealDark.copy(alpha = 0.6f),
            style = sp.stroke(2.5f),
        )
        drawPath(wing, color = INK, style = sp.stroke(4.5f))
    }
    drawRoundRect(
        color = tealDark,
        topLeft = sp.o(cx - 8f, cy - 9f),
        size = sp.sz(16f, 18f),
        cornerRadius = CornerRadius(sp.d(5f), sp.d(5f)),
    )
    drawRoundRect(
        color = INK,
        topLeft = sp.o(cx - 8f, cy - 9f),
        size = sp.sz(16f, 18f),
        cornerRadius = CornerRadius(sp.d(5f), sp.d(5f)),
        style = Stroke(width = sp.d(4f)),
    )
}
