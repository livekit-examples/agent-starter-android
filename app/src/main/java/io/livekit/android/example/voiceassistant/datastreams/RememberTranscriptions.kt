package io.livekit.android.example.voiceassistant.datastreams

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.livekit.android.room.Room
import io.livekit.android.room.datastream.TextStreamInfo
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.types.TranscriptionSegment
import io.livekit.android.room.types.merge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

private const val TRANSCRIPTION_TOPIC = "lk.transcription"

data class Transcription(
    val identity: Participant.Identity,
    val transcriptionSegment: TranscriptionSegment,
)

/**
 * Listens for incoming transcription data streams, and returns
 * all received transcriptions ordered by first received time.
 */
@Composable
fun rememberTranscriptions(room: Room): List<Transcription> {
    val coroutineScope = rememberCoroutineScope()
    val transcriptions = remember(room) { mutableStateMapOf<String, Transcription>() }
    val orderedTranscriptions = remember(transcriptions) {
        derivedStateOf {
            transcriptions.values.sortedBy { segment -> segment.transcriptionSegment.firstReceivedTime }
        }
    }

    DisposableEffect(room) {
        room.registerTextStreamHandler(TRANSCRIPTION_TOPIC) { receiver, identity ->
            coroutineScope.launch(Dispatchers.IO) {
                // Prepare for incoming transcription
                val segment = createTranscriptionSegment(streamInfo = receiver.info)
                val stringBuilder = StringBuilder()

                // Collect the incoming transcription stream.
                receiver.flow.collect { transcription ->
                    stringBuilder.append(transcription)

                    transcriptions.mergeNewSegments(
                        listOf(
                            Transcription(
                                identity = identity,
                                segment.copy(
                                    text = stringBuilder.toString(),
                                    lastReceivedTime = Date().time
                                )
                            )
                        )
                    )
                }
            }
        }

        onDispose {
            // Clean up the handler when done with it.
            room.unregisterTextStreamHandler(TRANSCRIPTION_TOPIC)
        }
    }

    return orderedTranscriptions.value
}

private fun createTranscriptionSegment(streamInfo: TextStreamInfo): TranscriptionSegment {
    return TranscriptionSegment(
        id = streamInfo.id,
        text = "",
        language = "",
        final = streamInfo.attributes["lk.transcription.final"]?.toBoolean() ?: false,
        firstReceivedTime = Date().time,
        lastReceivedTime = Date().time
    )
}

/**
 * Merges new transcriptions into an existing map.
 */
fun MutableMap<String, Transcription>.mergeNewSegments(newTranscriptions: Collection<Transcription>) {
    for (transcription in newTranscriptions) {
        val existingTranscription = get(transcription.transcriptionSegment.id)
        put(
            transcription.transcriptionSegment.id,
            Transcription(
                identity = transcription.identity,
                transcriptionSegment = existingTranscription?.transcriptionSegment.merge(transcription.transcriptionSegment)
            )
        )
    }
}