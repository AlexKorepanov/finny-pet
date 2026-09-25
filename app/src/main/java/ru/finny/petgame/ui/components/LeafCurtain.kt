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
import kotlin.math.hypot
import kotlin.random.Random

enum class LeafPhase { COVER, REVEAL }

private val leafColors = listOf(
    Color(0xFF7FA23A),
    Color(0xFF93B548),
    Color(0xFF6B8F2E),
    Color(0xFFA9C25A),
    Color(0xFF5E8129),
    Color(0xFFB9C96A),
)
private val leafVein = Color(0xFF4A6A1E)
private val curtainFill = Color(0xFF6E9334)

private class Leaf(
    val tx: Float,
    val ty: Float,
    val size: Float,
    val angle: Float,
    val color: Color,
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
                    color = leafColors[rnd.nextInt(leafColors.size)],
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
    Canvas(modifier = modifier.fillMaxSize().pointerInput(Unit) {}) {
        val fillAlpha = when (phase) {
            LeafPhase.COVER -> smooth((progress - 0.55f) / 0.35f)
            LeafPhase.REVEAL -> 1f - smooth(progress / 0.25f)
        }
        if (fillAlpha > 0f) drawRect(curtainFill.copy(alpha = fillAlpha))
        val w = size.width
        val h = size.height
        leaves.forEach { leaf ->
            val delay = if (phase == LeafPhase.COVER) leaf.coverDelay else leaf.revealDelay
            val t = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
            val targetX = leaf.tx * w
            val targetY = leaf.ty * h
            val x: Float
            val y: Float
            val rotation: Float
            if (phase == LeafPhase.COVER) {
                if (t <= 0f) return@forEach
                val e = FastOutSlowInEasing.transform(t)
                x = targetX + leaf.startDx * w * (1f - e)
                y = targetY + leaf.startDy * h * (1f - e)
                rotation = leaf.angle + leaf.spin * (1f - e)
            } else {
                if (t >= 1f) return@forEach
                val e = FastOutLinearInEasing.transform(t)
                val dx = leaf.tx - 0.5f
                val dy = leaf.ty - 0.5f
                val len = hypot(dx, dy).coerceAtLeast(0.05f)
                val push = 1.4f * e
                x = targetX + dx / len * w * push
                y = targetY + dy / len * h * push * 0.8f + h * 0.15f * e
                rotation = leaf.angle + leaf.spin * e
            }
            drawLeaf(Offset(x, y), leaf.size * w, leaf.wide, rotation, leaf.color)
        }
    }
}

private fun smooth(v: Float): Float {
    val x = v.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private fun DrawScope.drawLeaf(center: Offset, length: Float, wide: Float, rotation: Float, color: Color) {
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
