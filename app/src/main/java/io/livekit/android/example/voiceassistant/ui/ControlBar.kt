package io.livekit.android.example.voiceassistant.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

@Composable
private fun Modifier.enabledButtonModifier(enabled: Boolean): Modifier {
    return if (enabled) {
        this
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
    } else {
        this
    }
}

@OptIn(Beta::class)
@Composable
fun ControlBar(
    isMicEnabled: Boolean,
    onMicClick: () -> Unit,
    isCameraEnabled: Boolean,
    onCameraClick: () -> Unit,
    isScreenShareEnabled: Boolean,
    onScreenShareClick: () -> Unit,
    isChatEnabled: Boolean,
    onChatClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
            .padding(horizontal = 20.dp)
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
                .clickable(onClick = onMicClick)
                .height(48.dp)
                .weight(1f)
                .enabledButtonModifier(isMicEnabled)
        ) {
            Spacer(Modifier.size(8.dp))
            Icon(micIcon, "Toggle Microphone")
            AnimatedVisibility(isMicEnabled) {
                AudioBarVisualizer(
                    audioTrackRef = localAudioTrack,
                    brush = SolidColor(MaterialTheme.colorScheme.onBackground),
                    barCount = 3,
                    barWidth = 2.dp,
                    minHeight = 0.2f,
                    modifier = Modifier
                        .width(12.dp)
                        .height(20.dp)
                )
            }
            Spacer(Modifier.size(8.dp))
        }

        Spacer(Modifier.size(8.dp))

        val cameraIcon = if (isCameraEnabled) {
            Icons.Default.Videocam
        } else {
            Icons.Default.VideocamOff
        }

        IconButton(
            onClick = onCameraClick,
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .enabledButtonModifier(isCameraEnabled)
        ) {
            Icon(
                cameraIcon,
                "Toggle Camera"
            )
        }

        Spacer(Modifier.size(8.dp))

        IconButton(
            onClick = onScreenShareClick,
            modifier = buttonModifier
                .weight(1f)
                .enabledButtonModifier(isScreenShareEnabled)
        ) {
            Icon(Icons.Outlined.PresentToAll, "Toggle Screenshare")
        }

        Spacer(Modifier.size(8.dp))

        IconButton(
            onClick = onChatClick,
            modifier = buttonModifier
                .weight(1f)
                .enabledButtonModifier(isChatEnabled)
        ) {
            Icon(Icons.AutoMirrored.Filled.Chat, "Toggle Chat")
        }

        Spacer(Modifier.size(8.dp))

        IconButton(
            onClick = onExitClick,
            modifier = buttonModifier.weight(1f)
        ) {
            Icon(Icons.Default.CallEnd, "End Call", tint = Color.Red)
        }
    }
}

@Preview
@Composable
fun ControlBarPreview() {
    ControlBar(
        isMicEnabled = false,
        onMicClick = {},
        isCameraEnabled = false,
        onCameraClick = {},
        isScreenShareEnabled = false,
        onScreenShareClick = { },
        isChatEnabled = false,
        onChatClick = {},
        onExitClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
    )
}