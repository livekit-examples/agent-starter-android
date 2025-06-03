package io.livekit.android.example.voiceassistant.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.outlined.PresentToAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.livekit.android.annotations.Beta
import io.livekit.android.compose.local.requireRoom
import io.livekit.android.compose.state.rememberParticipantTrackReferences
import io.livekit.android.compose.ui.audio.AudioBarVisualizer
import io.livekit.android.room.track.Track

private val buttonModifier = Modifier
    .width(40.dp)
    .height(40.dp)

@OptIn(Beta::class)
@Composable
fun ControlBar(
    isMicEnabled: Boolean = true,
    onMicClick: () -> Unit = {},
    isCameraEnabled: Boolean = true,
    onCameraClick: () -> Unit = {},
    onScreenShareClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onExitClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {

        val micIcon = if (isMicEnabled) {
            Icons.Default.Mic
        } else {
            Icons.Default.MicOff
        }

        val room = requireRoom()
        val localAudioTrack = rememberParticipantTrackReferences(
            sources = listOf(Track.Source.MICROPHONE),
            passedParticipant = room.localParticipant
        ).firstOrNull()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxHeight()
                .defaultMinSize(minWidth = 52.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onMicClick)
        ) {
            Spacer(Modifier.size(8.dp))
            Icon(micIcon, "Toggle Microphone")
            AnimatedVisibility(isMicEnabled) {
                AudioBarVisualizer(
                    audioTrackRef = localAudioTrack,
                    brush = SolidColor(MaterialTheme.colorScheme.onBackground),
                    barCount = 3,
                    barWidth = 2.dp,
                    modifier = Modifier
                        .width(12.dp)
                        .height(32.dp)
                )
            }
            Spacer(Modifier.size(8.dp))
        }

        val cameraIcon = if (isCameraEnabled) {
            Icons.Default.Videocam
        } else {
            Icons.Default.VideocamOff
        }
        IconButton(onClick = onCameraClick, modifier = buttonModifier) {
            Icon(cameraIcon, "Toggle Camera")
        }
        IconButton(onClick = onScreenShareClick, modifier = buttonModifier) {
            Icon(Icons.Outlined.PresentToAll, "Toggle Screenshare")
        }
        IconButton(onClick = onChatClick, modifier = buttonModifier) {
            Icon(Icons.AutoMirrored.Filled.Chat, "Toggle Chat")
        }
        IconButton(onClick = onExitClick, modifier = buttonModifier) {
            Icon(Icons.Default.CallEnd, "End Call", tint = Color.Red)
        }
    }
}

@Preview
@Composable
fun ControlBarPreview() {
    ControlBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
    )
}