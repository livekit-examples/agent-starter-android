package io.livekit.android.example.voiceassistant.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import io.livekit.android.annotations.Beta
import io.livekit.android.compose.state.VoiceAssistant
import io.livekit.android.compose.state.rememberParticipantTrackReferences
import io.livekit.android.compose.ui.VideoTrackView
import io.livekit.android.compose.ui.audio.VoiceAssistantBarVisualizer
import io.livekit.android.room.track.Track

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

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        if (videoTrack != null) {
            VideoTrackView(
                trackReference = videoTrack,
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(videoTrack == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                VoiceAssistantBarVisualizer(
                    voiceAssistant = voiceAssistant,
                    barCount = 5,
                    brush = SolidColor(MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxSize(0.4f)
                )
            }
        }
    }
}