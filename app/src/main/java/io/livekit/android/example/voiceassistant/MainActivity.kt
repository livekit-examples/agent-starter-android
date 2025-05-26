@file:OptIn(Beta::class)

package io.livekit.android.example.voiceassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.github.ajalt.timberkt.Timber
import io.livekit.android.LiveKit
import io.livekit.android.annotations.Beta
import io.livekit.android.compose.local.RoomScope
import io.livekit.android.compose.state.rememberVoiceAssistant
import io.livekit.android.compose.ui.audio.VoiceAssistantBarVisualizer
import io.livekit.android.example.voiceassistant.datastreams.rememberTranscriptions
import io.livekit.android.example.voiceassistant.ui.UserTranscription
import io.livekit.android.example.voiceassistant.ui.theme.LiveKitVoiceAssistantExampleTheme
import io.livekit.android.util.LoggingLevel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LiveKit.loggingLevel = LoggingLevel.DEBUG
        requireNeededPermissions {
            requireToken { url, token ->
                setContent {
                    LiveKitVoiceAssistantExampleTheme(dynamicColor = false) {
                        Surface {
                            VoiceAssistant(
                                url,
                                token,
                                modifier = Modifier
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun VoiceAssistant(url: String, token: String, modifier: Modifier = Modifier) {
        ConstraintLayout(modifier = modifier) {
            RoomScope(
                url,
                token,
                audio = true,
                connect = true,
            ) { room ->
                val (audioVisualizer, chatLog) = createRefs()
                val voiceAssistant = rememberVoiceAssistant()

                val agentState = voiceAssistant.state
                // Optionally do something with the agent state.
                LaunchedEffect(key1 = agentState) {
                    Timber.i { "agent state: $agentState" }
                }

                // Amplitude visualization of the Assistant's voice track.
                VoiceAssistantBarVisualizer(
                    voiceAssistant = voiceAssistant,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .constrainAs(audioVisualizer) {
                            height = Dimension.percent(0.1f)
                            width = Dimension.percent(0.8f)

                            top.linkTo(parent.top, 8.dp)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        },
                    brush = SolidColor(MaterialTheme.colorScheme.onBackground)
                )

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
                    modifier = Modifier
                        .constrainAs(chatLog) {
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            height = Dimension.percent(0.9f)
                            width = Dimension.fillToConstraints
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
    }
}
