package io.livekit.android.example.voiceassistant.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.livekit.android.annotations.Beta
import io.livekit.android.compose.state.VoiceAssistant
import io.livekit.android.compose.state.rememberParticipantTrackReferences
import io.livekit.android.compose.ui.VideoTrackView
import io.livekit.android.compose.ui.audio.VoiceAssistantBarVisualizer
import io.livekit.android.example.voiceassistant.ui.anim.CircleReveal
import io.livekit.android.room.track.Track
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

val revealSpringSpec = spring<Float>(stiffness = Spring.StiffnessVeryLow)
val hideSpringSpec = spring<Float>(stiffness = Spring.StiffnessMedium)

@OptIn(Beta::class)
@Composable
fun AgentVisualization(
    voiceAssistant: VoiceAssistant,
    modifier: Modifier = Modifier
) {
    val agentIdentity = voiceAssistant.agent?.identity
    val videoTrack = if (agentIdentity != null) {
        rememberParticipantTrackReferences(
            sources = listOf(Track.Source.CAMERA),
            participantIdentity = voiceAssistant.agent?.identity,
        ).firstOrNull()
    } else {
        null
    }

    val revealed = videoTrack != null
    var delayedReveal by remember { mutableStateOf(false) }
    LaunchedEffect(revealed) {
        if (revealed) {
            delay(1000)
        }
        delayedReveal = revealed
    }

    Box(
        modifier = modifier
    ) {
        if (videoTrack != null) {
            VideoTrackView(
                trackReference = videoTrack,
                modifier = Modifier
                    .fillMaxSize()
            )
        }
        CircleReveal(
            revealed = delayedReveal,
            content = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val density = LocalDensity.current
                    var height by remember { mutableIntStateOf(0) }
                    val heightDp by remember {
                        derivedStateOf {
                            val heightDp = height / density.density
                            return@derivedStateOf max(1, (heightDp * 0.1f).roundToInt()).dp
                        }
                    }
                    VoiceAssistantBarVisualizer(
                        voiceAssistant = voiceAssistant,
                        barCount = 5,
                        minHeight = 0.1f,
                        barWidth = heightDp,
                        brush = SolidColor(MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxSize(0.4f)
                            .onSizeChanged { size ->
                                height = size.height
                            }
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            animationSpec = if (revealed) revealSpringSpec else hideSpringSpec,
        )
    }
}