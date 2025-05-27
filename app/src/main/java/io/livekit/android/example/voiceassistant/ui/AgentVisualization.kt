package io.livekit.android.example.voiceassistant.ui

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
import io.livekit.android.compose.ui.audio.VoiceAssistantBarVisualizer

@OptIn(Beta::class)
@Composable
fun AgentVisualization(
    voiceAssistant: VoiceAssistant,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        VoiceAssistantBarVisualizer(
            voiceAssistant = voiceAssistant,
            barCount = 5,
            brush = SolidColor(MaterialTheme.colorScheme.onBackground),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(0.4f)
        )
    }
}