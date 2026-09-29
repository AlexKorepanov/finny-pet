package ru.finny.petgame.ui.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import ru.finny.petgame.ui.theme.LocalTimeOfDay
import kotlin.math.hypot
import kotlin.random.Random

enum class LeafPhase { COVER, REVEAL }

// Цвета листьев, прожилок и сплошного занавеса берутся из времени суток (TimeOfDay).
private const val LEAF_COLOR_COUNT = 6

private class Leaf(
    val tx: Float,
    val ty: Float,
    val size: Float,
    val angle: Float,
    val colorIndex: Int,
    val startDx: Float,
    val startDy: Float,
    val spin: Float,
    val coverDelay: Float,
    val revealDelay: Float,
    val wide: Float,
)

private fun makeLeaves(): List<Leaf> {
    val rnd = Random(7)
    val cols = 5
    val rows = 10
    val leaves = mutableListOf<Leaf>()
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            repeat(2) {
                val tx = (c + 0.5f + (rnd.nextFloat() - 0.5f) * 0.9f) / cols
                val ty = (r + 0.5f + (rnd.nextFloat() - 0.5f) * 0.9f) / rows
                leaves += Leaf(
                    tx = tx,
                    ty = ty,
                    size = 0.30f + rnd.nextFloat() * 0.16f,
                    angle = rnd.nextFloat() * 360f,
                    colorIndex = rnd.nextInt(LEAF_COLOR_COUNT),
                    startDx = (rnd.nextFloat() - 0.5f) * 0.8f,
                    startDy = -(0.4f + ty + rnd.nextFloat() * 0.5f),
                    spin = (rnd.nextFloat() - 0.5f) * 540f,
                    coverDelay = rnd.nextFloat() * 0.35f,
                    revealDelay = rnd.nextFloat() * 0.3f,
                    wide = 0.36f + rnd.nextFloat() * 0.14f,
                )
            }
        }
    }
    return leaves.shuffled(rnd)
}

/**
 * Занавес из листьев между заставкой и игрой.
 * [COVER]: листья слетают сверху и закрывают экран, [progress] 0 → 1.
 * [REVEAL]: листья разлетаются от центра, открывая экран.
 */
@Composable
fun LeafCurtain(phase: LeafPhase, progress: Float, modifier: Modifier = Modifier) {
    val leaves = remember { makeLeaves() }
    val time = LocalTimeOfDay.current
    val leafVein = time.leafVein
    Canvas(modifier = modifier.fillMaxSize().pointerInput(Unit) {}) {
        val fillAlpha = when (phase) {
            LeafPhase.COVER -> smooth((progress - 0.55f) / 0.35f)
            LeafPhase.REVEAL -> 1f - smooth(progress / 0.25f)
        }
        if (fillAlpha > 0f) drawRect(time.curtainFill.copy(alpha = fillAlpha))
        val w = size.width
        val h = size.height
        leaves.forEach { leaf ->
            val pose = leafPose(leaf, phase, progress) ?: return@forEach
            drawLeaf(
                center = Offset(pose.x * w, pose.y * h),
                length = leaf.size * w,
                wide = leaf.wide,
                rotation = pose.rotation,
                color = time.leafColors[leaf.colorIndex],
                leafVein = leafVein,
            )
        }
    }
}

private class LeafPose(val x: Float, val y: Float, val rotation: Float)

/** Где лист в долях экрана при данном [progress]; null — листа не видно. */
private fun leafPose(leaf: Leaf, phase: LeafPhase, progress: Float): LeafPose? {
    val delay = if (phase == LeafPhase.COVER) leaf.coverDelay else leaf.revealDelay
    val t = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
    return if (phase == LeafPhase.COVER) {
        if (t <= 0f) return null
        val e = FastOutSlowInEasing.transform(t)
        LeafPose(
            x = leaf.tx + leaf.startDx * (1f - e),
            y = leaf.ty + leaf.startDy * (1f - e),
            rotation = leaf.angle + leaf.spin * (1f - e),
        )
    } else {
        if (t >= 1f) return null
        val e = FastOutLinearInEasing.transform(t)
        val dx = leaf.tx - 0.5f
        val dy = leaf.ty - 0.5f
        val len = hypot(dx, dy).coerceAtLeast(0.05f)
        val push = 1.4f * e
        LeafPose(
            x = leaf.tx + dx / len * push,
            y = leaf.ty + dy / len * push * 0.8f + 0.15f * e,
            rotation = leaf.angle + leaf.spin * e,
        )
    }
}

/**
 * «Шелест» листьев по кадрам: суммарная скорость листьев, которые сейчас на экране,
 * от 0 до 1 для каждого из [steps] равных отрезков фазы. По этой кривой строится
 * вибрация, поэтому она повторяет саму анимацию, а не идёт по своему таймеру.
 */
fun leafRustle(phase: LeafPhase, steps: Int): FloatArray {
    val leaves = makeLeaves()
    val out = FloatArray(steps)
    for (i in 0 until steps) {
        val p0 = i.toFloat() / steps
        val p1 = (i + 1).toFloat() / steps
        var energy = 0f
        for (leaf in leaves) {
            val a = leafPose(leaf, phase, p0)
            val b = leafPose(leaf, phase, p1) ?: continue
            if (b.x !in -0.1f..1.1f || b.y !in -0.1f..1.1f) continue
            // Лист только что появился — считаем, что он влетел с края кадра.
            val from = a ?: LeafPose(b.x, b.y - 0.05f, b.rotation)
            // Экран примерно вдвое выше, чем шире: вертикальный путь «длиннее».
            energy += hypot(b.x - from.x, (b.y - from.y) * 2f)
        }
        out[i] = energy
    }
    val max = out.maxOrNull()?.takeIf { it > 0f } ?: return out
    for (i in out.indices) out[i] /= max
    return out
}

private fun smooth(v: Float): Float {
    val x = v.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private fun DrawScope.drawLeaf(
    center: Offset,
    length: Float,
    wide: Float,
    rotation: Float,
    color: Color,
    leafVein: Color,
) {
    val half = length / 2f
    val bulge = length * wide
    translate(center.x, center.y) {
        rotate(rotation, pivot = Offset.Zero) {
            val blade = Path().apply {
                moveTo(0f, -half)
                cubicTo(bulge, -half * 0.45f, bulge * 0.8f, half * 0.55f, 0f, half)
                cubicTo(-bulge * 0.8f, half * 0.55f, -bulge, -half * 0.45f, 0f, -half)
                close()
            }
            drawPath(blade, color)
            drawPath(blade, leafVein.copy(alpha = 0.55f), style = Stroke(width = length * 0.025f))
            drawLine(
                color = leafVein.copy(alpha = 0.6f),
                start = Offset(0f, -half * 0.85f),
                end = Offset(0f, half * 1.15f),
                strokeWidth = length * 0.03f,
                cap = StrokeCap.Round,
            )
            for (k in 1..3) {
                val yy = -half * 0.5f + k * half * 0.35f
                drawLine(leafVein.copy(alpha = 0.35f), Offset(0f, yy), Offset(bulge * 0.55f, yy - half * 0.22f), length * 0.018f)
                drawLine(leafVein.copy(alpha = 0.35f), Offset(0f, yy), Offset(-bulge * 0.55f, yy - half * 0.22f), length * 0.018f)
            }
        }
    }
}
