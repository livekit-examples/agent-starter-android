package io.livekit.android.example.voiceassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private val buttonModifier = Modifier
    .width(40.dp)
    .height(40.dp)

@Composable
fun ControlBar(
    isMicEnabled: Boolean = true,
    onMicClick: () -> Unit = {},
    isCameraEnabled: Boolean = true,
    onCameraClick: () -> Unit = {},
    isScreenShareEnabled: Boolean = true,
    onScreenShareClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onExitClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    var height by remember { mutableIntStateOf(0) }
    val cornerRadius = height / 2f

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .onSizeChanged { height = it.height }
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surface)

            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {

        val micIcon = if (isMicEnabled) {
            Icons.Default.Mic
        } else {
            Icons.Default.MicOff
        }
        IconButton(onClick = onMicClick, modifier = buttonModifier) {
            Icon(micIcon, "Toggle Microphone")
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
            Icon(Icons.AutoMirrored.Filled.ScreenShare, "Toggle Camera")
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