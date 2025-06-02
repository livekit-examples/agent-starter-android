package io.livekit.android.example.voiceassistant.ui.anim

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.max

@Composable
fun CircleRevealAnimation(
    revealed: Boolean,
    initialContent: @Composable () -> Unit,
    revealContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessLow),
) {
    val clipPercent by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        label = "Circle Reveal Percent",
        animationSpec = animationSpec,
    )

    Box(modifier = modifier) {
        // Draw initial content if not fully revealed.
        if (clipPercent < 1f) {
            initialContent()
        }

        // Draw revealed content if any revealed.
        if (clipPercent > 0f) {
            val path = remember { Path() }

            val clipModifier = if (clipPercent < 1f) {
                Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        path.rewind()
                        val largestSide = max(size.width, size.height)
                        path.addOval(
                            Rect(
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = largestSide * clipPercent
                            )
                        )

                        clipPath(path) { this@drawWithContent.drawContent() }
                    }
            } else {
                Modifier.fillMaxSize()
            }

            Box(modifier = clipModifier) {
                revealContent()
            }
        }
    }
}