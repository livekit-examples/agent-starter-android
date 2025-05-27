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
        val displayTranscriptions = transcriptions.asReversed()
        val lazyListState = rememberLazyListState()

        LaunchedEffect(transcriptions) {
            lazyListState.animateScrollToItem(0)
        }
        LazyColumn(
            userScrollEnabled = true,
            state = lazyListState,
            reverseLayout = true,
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
                    if (transcription.identity == room.localParticipant.identity) {
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