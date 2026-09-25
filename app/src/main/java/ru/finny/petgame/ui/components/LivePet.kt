package ru.finny.petgame.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.finny.petgame.ui.model.PetLook
import kotlin.random.Random

/**
 * Питомец на главном экране: дышит, покачивается (бодрее при хорошем настроении),
 * моргает и подпрыгивает от нажатия. Без анимаций — обычный неподвижный спрайт.
 */
@Composable
fun LivePet(
    look: PetLook,
    stage: Int,
    mood: Int,
    modifier: Modifier = Modifier,
) {
    if (!LocalAnimationsEnabled.current) {
        PetSprite(look = look, stage = stage, modifier = modifier)
        return
    }
    val cheerful = mood >= 60
    val idle = rememberInfiniteTransition(label = "pet-idle")
    val breath by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    val sway by idle.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(if (cheerful) 1500 else 2600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "sway",
    )
    val swayDegrees = if (cheerful) 2.5f else 1.2f

    var eyesClosed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2400, 5200))
            eyesClosed = true
            delay(130)
            eyesClosed = false
            if (Random.nextInt(4) == 0) {
                delay(160)
                eyesClosed = true
                delay(120)
                eyesClosed = false
            }
        }
    }

    val hop = remember { Animatable(0f) }
    val squash = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val interaction = remember { MutableInteractionSource() }

    PetSprite(
        look = look,
        stage = stage,
        eyesClosed = eyesClosed,
        modifier = modifier
            .clickable(interactionSource = interaction, indication = null) {
                if (hop.isRunning) return@clickable
                scope.launch {
                    squash.animateTo(1f, tween(90))
                    launch { squash.animateTo(-0.6f, tween(160)) }
                    hop.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
                    hop.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
                    squash.animateTo(0f, spring(dampingRatio = 0.4f))
                }
            }
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 1f)
                val breathe = breath * 0.025f
                scaleY = 1f + breathe - squash.value * 0.12f
                scaleX = 1f - breathe * 0.4f + squash.value * 0.08f
                rotationZ = sway * swayDegrees
                translationY = -hop.value * size.height * 0.18f
            },
    )
}
