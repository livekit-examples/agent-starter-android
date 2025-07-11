package io.livekit.android.example.voiceassistant.screen

import android.app.Activity
import android.content.Context.MEDIA_PROJECTION_SERVICE
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstrainScope
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import androidx.constraintlayout.compose.layoutId
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import io.livekit.android.annotations.Beta
import io.livekit.android.compose.local.RoomScope
import io.livekit.android.compose.state.rememberParticipantTrackReferences
import io.livekit.android.compose.state.rememberVoiceAssistant
import io.livekit.android.compose.ui.VideoTrackView
import io.livekit.android.example.voiceassistant.datastreams.rememberTranscriptions
import io.livekit.android.example.voiceassistant.rememberEnableCamera
import io.livekit.android.example.voiceassistant.rememberEnableMic
import io.livekit.android.example.voiceassistant.requirePermissions
import io.livekit.android.example.voiceassistant.ui.AgentVisualization
import io.livekit.android.example.voiceassistant.ui.ChatBar
import io.livekit.android.example.voiceassistant.ui.ChatLog
import io.livekit.android.example.voiceassistant.ui.ControlBar
import io.livekit.android.example.voiceassistant.viewmodel.VoiceAssistantViewModel
import io.livekit.android.room.datastream.StreamTextOptions
import io.livekit.android.room.datastream.TextStreamInfo
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.screencapture.ScreenCaptureParams
import io.livekit.android.util.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class VoiceAssistantRoute(val url: String, val token: String)

@Composable
fun VoiceAssistantScreen(
    viewModel: VoiceAssistantViewModel,
    onEndCall: () -> Unit,
) {
    VoiceAssistant(
        viewModel = viewModel,
        modifier = Modifier.fillMaxSize(),
        onEndCall = onEndCall
    )
}

@OptIn(Beta::class, ExperimentalPermissionsApi::class)
@Composable
fun VoiceAssistant(
    viewModel: VoiceAssistantViewModel,
    modifier: Modifier = Modifier,
    onEndCall: () -> Unit
) {
    var requestedAudio by remember { mutableStateOf(true) } // Turn on audio by default.
    var requestedVideo by remember { mutableStateOf(false) }

    requirePermissions(requestedAudio, requestedVideo)

    RoomScope(
        passedRoom = viewModel.room,
        audio = rememberEnableMic(requestedAudio),
        video = rememberEnableCamera(requestedVideo),
        connect = true,
        disconnectOnDispose = false,
    ) { room ->

        var chatVisible by remember { mutableStateOf(false) }

        val isMicEnabled by room.localParticipant::isMicrophoneEnabled.flow.collectAsState()
        val isCameraEnabled by room.localParticipant::isCameraEnabled.flow.collectAsState()
        val isScreenShareEnabled by room.localParticipant::isScreenShareEnabled.flow.collectAsState()
        val transcriptionsState = rememberTranscriptions(room)

        val constraints = getConstraints(chatVisible, isCameraEnabled, isScreenShareEnabled)
        ConstraintLayout(
            constraintSet = constraints,
            modifier = modifier,
            animateChangesSpec = spring()
        ) {
            val coroutineScope = rememberCoroutineScope { Dispatchers.IO }

            ChatLog(
                room = room,
                transcriptions = transcriptionsState.transcriptions.value,
                modifier = Modifier.layoutId(LAYOUT_ID_CHAT_LOG)
            )

            var message by rememberSaveable {
                mutableStateOf("")
            }
            ChatBar(
                value = message,
                onValueChange = { message = it },
                onChatSend = { msg ->
                    coroutineScope.launch {
                        val sender = room.localParticipant.streamText(
                            StreamTextOptions(
                                operationType = TextStreamInfo.OperationType.CREATE,
                                topic = "lk.chat"
                            )
                        )
                        sender.write(msg)
                        sender.close()

                        val identity = room.localParticipant.identity ?: return@launch
                        transcriptionsState.addTranscription(identity, msg)
                    }
                    message = ""
                },
                modifier = Modifier.layoutId(LAYOUT_ID_CHAT_BAR)
            )

            // Amplitude visualization of the Assistant's voice track.
            val voiceAssistant = rememberVoiceAssistant()
            val agentBorderAlpha by animateFloatAsState(if (chatVisible) 1f else 0f, label = "agentBorderAlpha")
            AgentVisualization(
                voiceAssistant = voiceAssistant,
                modifier = Modifier
                    .layoutId(LAYOUT_ID_AGENT)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = agentBorderAlpha), RoundedCornerShape(8.dp))
            )

            val context = LocalContext.current
            val screenSharePermissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    val resultCode = result.resultCode
                    val data = result.data
                    if (resultCode != Activity.RESULT_OK || data == null) {
                        return@rememberLauncherForActivityResult
                    }
                    coroutineScope.launch {
                        // Agents only support one video stream at a time.
                        requestedVideo = false
                        room.localParticipant.setScreenShareEnabled(true, ScreenCaptureParams(data))
                    }
                }

            ControlBar(
                isMicEnabled = isMicEnabled,
                onMicClick = { requestedAudio = !requestedAudio },
                isCameraEnabled = isCameraEnabled,
                onCameraClick = {
                    requestedVideo = !requestedVideo
                    if (requestedVideo) {
                        // Agents only support one video stream at a time.
                        coroutineScope.launch { room.localParticipant.setScreenShareEnabled(false) }
                    }
                },
                isScreenShareEnabled = isScreenShareEnabled,
                onScreenShareClick = {
                    if (!isScreenShareEnabled) {
                        // Screenshare permission needs to be requested each time.
                        val mediaProjectionManager = context.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        screenSharePermissionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                    } else {
                        coroutineScope.launch { room.localParticipant.setScreenShareEnabled(false) }
                    }
                },
                isChatEnabled = chatVisible,
                onChatClick = { chatVisible = !chatVisible },
                onExitClick = onEndCall,
                modifier = Modifier
                    .layoutId(LAYOUT_ID_CONTROL_BAR)
            )

            val cameraAlpha by animateFloatAsState(targetValue = if (isCameraEnabled) 1f else 0f, label = "Camera Alpha")
            val cameraTrack = rememberParticipantTrackReferences(
                listOf(Track.Source.CAMERA),
                passedParticipant = room.localParticipant
            ).firstOrNull()
            Box(
                modifier = Modifier
                    .layoutId(LAYOUT_ID_CAMERA)
                    .clickable {
                        room.localParticipant
                            .getOrCreateDefaultVideoTrack()
                            .switchCamera()
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .alpha(cameraAlpha)
            ) {
                VideoTrackView(
                    trackReference = cameraTrack,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .fillMaxWidth(.35f)
                        .aspectRatio(1f)
                ) {
                    Icon(
                        Icons.Default.Cameraswitch,
                        tint = Color.White.copy(alpha = 0.7f),
                        contentDescription = "Flip Camera",
                        modifier = Modifier.fillMaxSize(0.6f)
                    )
                }
            }

            val screenShareAlpha by animateFloatAsState(targetValue = if (isScreenShareEnabled) 1f else 0f, label = "Screen Share Alpha")
            val screenShareTrack = rememberParticipantTrackReferences(
                listOf(Track.Source.SCREEN_SHARE),
                passedParticipant = room.localParticipant
            ).firstOrNull()
            VideoTrackView(
                trackReference = screenShareTrack,
                modifier = Modifier
                    .layoutId(LAYOUT_ID_SCREENSHARE)
                    .clip(RoundedCornerShape(8.dp))
                    .alpha(screenShareAlpha)
            )
        }
    }
}


private const val LAYOUT_ID_AGENT = "agentVisualizer"
private const val LAYOUT_ID_CHAT_LOG = "chatLog"
private const val LAYOUT_ID_CONTROL_BAR = "controlBar"
private const val LAYOUT_ID_CHAT_BAR = "chatBar"
private const val LAYOUT_ID_CAMERA = "camera"
private const val LAYOUT_ID_SCREENSHARE = "screenshare"

private fun getConstraints(chatVisible: Boolean, cameraVisible: Boolean, screenShareVisible: Boolean) = ConstraintSet {
    val (agentVisualizer, chatLog, controlBar, chatBar, camera, screenShare) = createRefsFor(
        LAYOUT_ID_AGENT,
        LAYOUT_ID_CHAT_LOG,
        LAYOUT_ID_CONTROL_BAR,
        LAYOUT_ID_CHAT_BAR,
        LAYOUT_ID_CAMERA,
        LAYOUT_ID_SCREENSHARE,
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

    constrain(chatBar) {
        bottom.linkTo(controlBar.top, 16.dp)
        start.linkTo(parent.start, 16.dp)
        end.linkTo(parent.end, 16.dp)
        width = Dimension.fillToConstraints
        height = Dimension.wrapContent
    }

    constrain(controlBar) {
        bottom.linkTo(parent.bottom, 10.dp)
        start.linkTo(parent.start, 16.dp)
        end.linkTo(parent.end, 16.dp)

        width = Dimension.fillToConstraints
        height = Dimension.value(60.dp)
    }

    if (chatVisible) {
        val chain = createHorizontalChain(agentVisualizer, screenShare, camera, chainStyle = ChainStyle.Spread)

        constrain(chain) {
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        }

        fun ConstrainScope.itemConstraints(visible: Boolean = true) {
            top.linkTo(parent.top)
            bottom.linkTo(chatTopGuideline)
            width = Dimension.percent(0.3f)
            height = Dimension.fillToConstraints
            visibility = if (visible) Visibility.Visible else Visibility.Gone
        }
        constrain(agentVisualizer) {
            itemConstraints()
        }
        constrain(camera) {
            itemConstraints(cameraVisible)
        }
        constrain(screenShare) {
            itemConstraints(screenShareVisible)
        }
    } else {
        constrain(agentVisualizer) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            height = Dimension.fillToConstraints
            width = Dimension.fillToConstraints
        }
        constrain(camera) {
            end.linkTo(parent.end, 16.dp)
            bottom.linkTo(controlBar.top, 16.dp)
            width = Dimension.percent(0.25f)
            height = Dimension.percent(0.2f)
            visibility = if (cameraVisible) Visibility.Visible else Visibility.Gone
        }
        constrain(screenShare) {
            if (cameraVisible) {
                end.linkTo(camera.start, 16.dp)
            } else {
                end.linkTo(parent.end, 16.dp)
            }
            bottom.linkTo(controlBar.top, 16.dp)
            width = Dimension.percent(0.25f)
            height = Dimension.percent(0.2f)
            visibility = if (screenShareVisible) Visibility.Visible else Visibility.Gone
        }
    }
}