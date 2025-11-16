package io.livekit.android.example.voiceassistant.ui


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.livekit.android.compose.types.ReceivedMessage

/**
 * Composable for displaying an individual user message.
 */
@Composable
fun UserMessage(
    message: ReceivedMessage,
    modifier: Modifier = Modifier
) {
    val state = remember {
        MutableTransitionState(false).apply {
            // Start the animation immediately.
            targetState = true
        }
    }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(),
        modifier = modifier
    ) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp, 2.dp, 8.dp, 8.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Text(
                text = message.message,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}