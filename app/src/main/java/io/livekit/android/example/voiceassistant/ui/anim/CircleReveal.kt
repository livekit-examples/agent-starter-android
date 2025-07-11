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
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.max

@Composable
fun CircleReveal(
    revealed: Boolean,
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessLow),
    content: @Composable () -> Unit,
) {

    val clipPercent by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        label = "Circle Reveal Percent",
        animationSpec = animationSpec,
    )

    Box(modifier = modifier) {
        // Draw content if not fully revealed.
        if (clipPercent < 1f) {
            val path = remember { Path() }

            val clipModifier = if (clipPercent > 0f) {
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

                        clipPath(path, clipOp = ClipOp.Difference) { this@drawWithContent.drawContent() }
                    }
            } else {
                Modifier.fillMaxSize()
            }
            Box(modifier = clipModifier) {
                content()
            }
        }
    }
}