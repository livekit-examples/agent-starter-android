@file:OptIn(Beta::class)

package io.livekit.android.example.voiceassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import io.livekit.android.LiveKit
import io.livekit.android.annotations.Beta
import io.livekit.android.compose.local.RoomScope
import io.livekit.android.compose.state.rememberVoiceAssistant
import io.livekit.android.example.voiceassistant.ui.AgentVisualization
import io.livekit.android.example.voiceassistant.ui.ChatBar
import io.livekit.android.example.voiceassistant.ui.ChatLog
import io.livekit.android.example.voiceassistant.ui.ControlBar
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
        RoomScope(
            url,
            token,
            audio = true,
            connect = true,
        ) { room ->
            var chatVisible by remember { mutableStateOf(false) }
            val constraints = getConstraints(chatVisible)
            ConstraintLayout(
                constraintSet = constraints,
                modifier = modifier,
                animateChangesSpec = spring()
            ) {
                // Amplitude visualization of the Assistant's voice track.
                val voiceAssistant = rememberVoiceAssistant()

                ChatLog(
                    room = room,
                    modifier = Modifier.layoutId(LAYOUT_ID_CHAT_LOG)
                )

                ChatBar(
                    onChatSend = {},
                    modifier = Modifier.layoutId(LAYOUT_ID_CHAT_BAR)
                )

                // Amplitude visualization of the Assistant's voice track.
                val agentBorderAlpha by animateFloatAsState(if (chatVisible) 1f else 0f, label = "agentBorderAlpha")
                AgentVisualization(
                    voiceAssistant = voiceAssistant,
                    modifier = Modifier
                        .layoutId(LAYOUT_ID_AGENT)
                        .border(1.dp, Color.Gray.copy(alpha = agentBorderAlpha))
                )

                ControlBar(
                    isMicEnabled = room.localParticipant.isCameraEnabled(),
                    onChatClick = { chatVisible = !chatVisible },
                    onExitClick = { finish() },
                    modifier = Modifier
                        .layoutId(LAYOUT_ID_CONTROL_BAR)
                )

            }
        }
    }
}

private const val LAYOUT_ID_AGENT = "agentVisualizer"
private const val LAYOUT_ID_CHAT_LOG = "chatLog"
private const val LAYOUT_ID_CONTROL_BAR = "controlBar"
private const val LAYOUT_ID_CHAT_BAR = "chatBar"

private fun getConstraints(chatVisible: Boolean) = ConstraintSet {
    val (agentVisualizer, chatLog, controlBar, chatBar) = createRefsFor(
        LAYOUT_ID_AGENT,
        LAYOUT_ID_CHAT_LOG,
        LAYOUT_ID_CONTROL_BAR,
        LAYOUT_ID_CHAT_BAR
    )
    val chatTopGuideline = createGuidelineFromTop(0.2f)

    constrain(chatLog) {
        top.linkTo(chatTopGuideline)
        bottom.linkTo(chatBar.top)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
    }

    constrain(controlBar) {
        bottom.linkTo(parent.bottom, 10.dp)
        start.linkTo(parent.start, 20.dp)
        end.linkTo(parent.end, 20.dp)

        width = Dimension.fillToConstraints
        height = Dimension.value(40.dp)
    }

    constrain(chatBar) {
        bottom.linkTo(controlBar.top)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
        width = Dimension.fillToConstraints
        height = Dimension.wrapContent
    }

    constrain(agentVisualizer) {
        top.linkTo(parent.top)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
        height = Dimension.fillToConstraints

        if (!chatVisible) {
            bottom.linkTo(parent.bottom)
            width = Dimension.fillToConstraints
        } else {
            bottom.linkTo(chatTopGuideline)
            width = Dimension.percent(0.3f)
        }
    }
}