@file:OptIn(Beta::class)

package io.livekit.android.example.voiceassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import io.livekit.android.LiveKit
import io.livekit.android.annotations.Beta
import io.livekit.android.compose.local.RoomScope
import io.livekit.android.compose.state.rememberVoiceAssistant
import io.livekit.android.example.voiceassistant.ui.AgentVisualization
import io.livekit.android.example.voiceassistant.ui.ChatLog
import io.livekit.android.example.voiceassistant.ui.ControlBar
import io.livekit.android.example.voiceassistant.ui.MessageBar
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
                        Scaffold { innerPadding ->
                            VoiceAssistant(
                                url,
                                token,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
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
                val (topSpacer, agentVisualizer, chatLog, controlBar, messageBar) = createRefs()
                val voiceAssistant = rememberVoiceAssistant()

                // Spacer for the ChatLog to link to.
                Spacer(
                    modifier = Modifier
                        .background(Color.Green)
                        .constrainAs(topSpacer) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                            height = Dimension.percent(0.3f)
                        }
                )

                ChatLog(
                    room = room,
                    modifier = Modifier
                        .background(Color.Red)
                        .constrainAs(chatLog) {
                            top.linkTo(topSpacer.bottom)
                            bottom.linkTo(messageBar.top)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                            height = Dimension.fillToConstraints
                        }
                )

                MessageBar(
                    modifier = Modifier
                        .background(Color.Magenta)
                        .constrainAs(messageBar) {
                            bottom.linkTo(controlBar.top)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                            height = Dimension.value(40.dp)
                        }
                )

                var chatVisible by remember { mutableStateOf(false) }
                // Amplitude visualization of the Assistant's voice track.
                AgentVisualization(
                    voiceAssistant = voiceAssistant,
                    modifier = Modifier
                        .animateContentSize()
                        .background(Color.Yellow)
                        .constrainAs(agentVisualizer) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            if (!chatVisible) {
                                bottom.linkTo(parent.bottom)
                            } else {
                                bottom.linkTo(topSpacer.bottom)
                            }
                            width = Dimension.fillToConstraints
                            height = Dimension.fillToConstraints
                        }
                )


                ControlBar(
                    onChatClick = { chatVisible = !chatVisible },
                    modifier = Modifier
                        .constrainAs(controlBar) {
                            bottom.linkTo(parent.bottom, 10.dp)
                            start.linkTo(parent.start, 20.dp)
                            end.linkTo(parent.end, 20.dp)

                            width = Dimension.fillToConstraints
                            height = Dimension.value(40.dp)
                        }
                )

            }
        }
    }
}
