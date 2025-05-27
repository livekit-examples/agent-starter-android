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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.livekit.android.example.voiceassistant.datastreams.rememberTranscriptions
import io.livekit.android.room.Room

@Composable
fun ChatLog(room: Room, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        // Get and display the transcriptions.
        val transcriptions = rememberTranscriptions(room)
        val lazyListState = rememberLazyListState()

        val lastUserTranscription by remember(transcriptions) {
            derivedStateOf {
                transcriptions.lastOrNull { it.identity == room.localParticipant.identity }
            }
        }

        val lastAgentSegment by remember(transcriptions) {
            derivedStateOf {
                transcriptions.lastOrNull { it.identity != room.localParticipant.identity }
            }
        }

        val displayTranscriptions by remember(lastUserTranscription, lastAgentSegment) {
            derivedStateOf {
                listOfNotNull(lastUserTranscription, lastAgentSegment)
            }
        }

        LazyColumn(
            userScrollEnabled = true,
            state = lazyListState,
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight()
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
                    if (transcription == lastUserTranscription) {
                        UserTranscription(
                            transcription = transcription.transcriptionSegment,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    } else {
                        Text(
                            text = transcription.transcriptionSegment.text,
                            fontWeight = FontWeight.Light,
                            fontSize = 20.sp,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                    }
                }
            }
        }
    }
}