package io.livekit.android.example.voiceassistant.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import io.livekit.android.compose.ui.ScaleType
import io.livekit.android.compose.ui.VideoTrackView
import io.livekit.android.compose.ui.audio.VoiceAssistantBarVisualizer
import io.livekit.android.example.voiceassistant.ui.anim.CircleReveal
import io.livekit.android.room.track.Track
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

    // TODO: REMOVE
    // For non agent testing
//    val room = requireRoom()
//    val participants by room::remoteParticipants.flow.collectAsState()
//    val participant = participants.values.firstOrNull()
//    val videoTrack: TrackReference?
//    if (participant != null) {
//        val agentIdentity by participant::identity.flow.collectAsState()
//        if (agentIdentity != null) {
//            videoTrack = rememberParticipantTrackReferences(
//                sources = listOf(Track.Source.CAMERA),
//                participantIdentity = agentIdentity,
//            ).firstOrNull()
//        } else {
//            videoTrack = null
//        }
//    } else {
//        videoTrack = null
//    }
    val agentIdentity = voiceAssistant.agent?.identity

    val videoTrack = if (agentIdentity != null) {
        rememberParticipantTrackReferences(
            sources = listOf(Track.Source.CAMERA),
            participantIdentity = agentIdentity,
        ).firstOrNull()
    } else {
        null
    }

    var hasFirstFrameRendered by remember(videoTrack) { mutableStateOf(false) }
    val revealed = videoTrack != null && hasFirstFrameRendered

    Box(modifier = modifier) {
        if (videoTrack != null) {
            VideoTrackView(
                trackReference = videoTrack,
                scaleType = ScaleType.FitInside,
                onFirstFrameRendered = { hasFirstFrameRendered = true },
                modifier = Modifier
                    .fillMaxSize(),
            )
        }
        CircleReveal(
            revealed = revealed,
            content = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val density = LocalDensity.current
                    var width by remember { mutableIntStateOf(0) }
                    var height by remember { mutableIntStateOf(0) }

                    val barWidth by remember {
                        derivedStateOf {
                            val widthDp = width / density.density
                            return@derivedStateOf max(1, (widthDp * 0.157f).roundToInt()).dp
                        }
                    }
                    val barMinHeightPercent by remember {
                        derivedStateOf {
                            val widthPx = barWidth.value * density.density
                            return@derivedStateOf max(0f, (widthPx / height))
                        }
                    }
                    VoiceAssistantBarVisualizer(
                        voiceAssistant = voiceAssistant,
                        barCount = 5,
                        minHeight = barMinHeightPercent,
                        barWidth = barWidth,
                        brush = SolidColor(MaterialTheme.colorScheme.onBackground),
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .fillMaxHeight(0.22f)
                            .onSizeChanged { size ->
                                width = size.width
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