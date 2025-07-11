package io.livekit.android.example.voiceassistant.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.livekit.android.example.voiceassistant.datastreams.Transcription
import io.livekit.android.room.Room

@Composable
fun ChatLog(room: Room, transcriptions: List<Transcription>, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        // Get and display the transcriptions.
        val displayTranscriptions = transcriptions.asReversed()
        val lazyListState = rememberLazyListState()

        // Scroll to bottom when new transcriptions come in.
        LaunchedEffect(transcriptions.count()) {
            lazyListState.animateScrollToItem(0)
        }
        LazyColumn(
            userScrollEnabled = true,
            state = lazyListState,
            reverseLayout = true,
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    // Fade top
                    val colors = arrayOf(
                        0.0f to Color.Transparent,
                        0.15f to Color.Black,
                        1.0f to Color.Black,
                    )
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = colors
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            items(
                items = displayTranscriptions,
                key = { transcription -> transcription.transcriptionSegment.id },
            ) { transcription ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .animateItem()
                ) {
                    if (transcription.identity == room.localParticipant.identity) {
                        UserTranscription(
                            transcription = transcription.transcriptionSegment,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    } else {
                        Text(
                            text = transcription.transcriptionSegment.text,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                    }
                }
            }
        }
    }
}