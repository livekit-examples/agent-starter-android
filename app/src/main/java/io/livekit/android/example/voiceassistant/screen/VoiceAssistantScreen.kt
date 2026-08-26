package io.livekit.android.example.voiceassistant.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import io.livekit.android.annotations.Beta
import io.livekit.android.compose.local.SessionScope
import io.livekit.android.compose.local.requireRoom
import io.livekit.android.compose.state.Agent
import io.livekit.android.compose.state.Session
import io.livekit.android.compose.state.SessionConnectOptions
import io.livekit.android.compose.state.SessionConnectTrackOptions
import io.livekit.android.compose.state.SessionMessages
import io.livekit.android.compose.state.SessionOptions
import io.livekit.android.compose.state.rememberAgent
import io.livekit.android.compose.state.rememberLocalMedia
import io.livekit.android.compose.state.rememberSession
import io.livekit.android.compose.state.rememberSessionMessages
import io.livekit.android.compose.types.LocalMedia
import io.livekit.android.compose.types.ReceivedAgentTranscriptionMessage
import io.livekit.android.compose.types.ReceivedChatMessage
import io.livekit.android.compose.types.ReceivedMessage
import io.livekit.android.compose.types.ReceivedUserTranscriptionMessage
import io.livekit.android.events.RoomEvent
import io.livekit.android.example.voiceassistant.APPROVAL_REQUEST_TOPIC
import io.livekit.android.example.voiceassistant.APPROVAL_RESPONSE_TOPIC
import io.livekit.android.example.voiceassistant.ApprovalRequest
import io.livekit.android.example.voiceassistant.approvalResponseJson
import io.livekit.android.example.voiceassistant.parseApprovalRequest
import io.livekit.android.example.voiceassistant.realtime.CONTROL_TOPIC
import io.livekit.android.example.voiceassistant.realtime.ControlPacket
import io.livekit.android.example.voiceassistant.realtime.HermesCommand
import io.livekit.android.example.voiceassistant.realtime.InputIntent
import io.livekit.android.example.voiceassistant.realtime.LatencyTracker
import io.livekit.android.example.voiceassistant.realtime.STATUS_TOPIC
import io.livekit.android.example.voiceassistant.realtime.StatusPacket
import io.livekit.android.example.voiceassistant.realtime.TimelineMessage
import io.livekit.android.example.voiceassistant.realtime.TimelineUpdate
import io.livekit.android.example.voiceassistant.realtime.controlPacketJson
import io.livekit.android.example.voiceassistant.realtime.parseInput
import io.livekit.android.example.voiceassistant.realtime.parseStatusPacket
import io.livekit.android.example.voiceassistant.realtime.reduceTimeline
import io.livekit.android.example.voiceassistant.realtime.suggestInputs
import io.livekit.android.example.voiceassistant.rememberCanEnableMic
import io.livekit.android.example.voiceassistant.requirePermissions
import io.livekit.android.example.voiceassistant.ui.ChatBar
import io.livekit.android.example.voiceassistant.ui.ChatLog
import io.livekit.android.example.voiceassistant.viewmodel.VoiceAssistantViewModel
import io.livekit.android.room.Room
import io.livekit.android.room.datastream.StreamTextOptions
import io.livekit.android.room.track.DataPublishReliability
import io.livekit.android.room.track.RemoteAudioTrack
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable

private const val AGENT_NAME = "hermes-voice"

@Serializable
data class VoiceAssistantRoute(
    val tokenServerId: String,
    val hardcodedUrl: String,
    val hardcodedToken: String
)

interface HermesSessionController {
    val isConnected: Boolean
    val isReconnecting: Boolean

    suspend fun start(microphoneEnabled: Boolean)

    suspend fun setMicrophoneEnabled(enabled: Boolean)

    suspend fun setAgentVolume(volume: Double)
}

interface HermesChatTransport {
    suspend fun sendMessage(text: String, localId: String, operationId: String): String

    suspend fun sendControl(packet: ControlPacket): Boolean
}

private object NoOpHermesChatTransport : HermesChatTransport {
    override suspend fun sendMessage(
        text: String,
        localId: String,
        operationId: String
    ): String = localId

    override suspend fun sendControl(packet: ControlPacket): Boolean = true
}

@OptIn(Beta::class)
private class LiveKitHermesSessionController(
    private val session: Session,
    private val localMedia: LocalMedia,
    private val agent: Agent
) : HermesSessionController {
    override val isConnected: Boolean
        get() = session.isConnected
    override val isReconnecting: Boolean
        get() = session.isReconnecting

    override suspend fun start(microphoneEnabled: Boolean) {
        session.start(
            SessionConnectOptions(
                tracks = SessionConnectTrackOptions(
                    microphoneEnabled = microphoneEnabled,
                    usePreconnectBuffer = microphoneEnabled
                )
            )
        ).getOrThrow()
    }

    override suspend fun setMicrophoneEnabled(enabled: Boolean) {
        localMedia.setMicrophoneEnabled(enabled)
    }

    override suspend fun setAgentVolume(volume: Double) {
        if (agent.audioTrack == null) {
            withTimeoutOrNull(5_000) { agent.waitUntilMicrophone() }
        }
        (agent.audioTrack?.publication?.track as? RemoteAudioTrack)?.setVolume(volume)
    }
}

@OptIn(Beta::class)
private class LiveKitHermesChatTransport(
    private val messages: SessionMessages,
    private val room: Room
) : HermesChatTransport {
    override suspend fun sendMessage(
        text: String,
        localId: String,
        operationId: String
    ): String {
        val message = messages.send(
            text,
            StreamTextOptions(
                topic = "lk.chat",
                attributes = mapOf("local_id" to localId, "op_id" to operationId)
            )
        ).getOrThrow()
        return message.id
    }

    override suspend fun sendControl(packet: ControlPacket): Boolean {
        val payload = controlPacketJson(packet) ?: return false
        return room.localParticipant.publishData(
            payload.encodeToByteArray(),
            reliability = DataPublishReliability.RELIABLE,
            topic = CONTROL_TOPIC
        ).isSuccess
    }
}

@Composable
fun VoiceAssistantScreen(viewModel: VoiceAssistantViewModel) {
    VoiceAssistant(
        viewModel = viewModel,
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(Beta::class, ExperimentalPermissionsApi::class)
@Composable
fun VoiceAssistant(
    viewModel: VoiceAssistantViewModel,
    modifier: Modifier = Modifier
) {
    var requestedAudio by remember { mutableStateOf(false) }
    requirePermissions(requestedAudio, false)
    val canEnableMic by rememberCanEnableMic()
    val session = rememberSession(
        tokenSource = viewModel.tokenSource,
        options = SessionOptions(
            room = viewModel.room,
            tokenRequestOptions = viewModel.tokenRequestOptions(AGENT_NAME)
        )
    )
    val context = LocalContext.current

    SessionScope(session = session) {
        DisposableEffect(Unit) {
            onDispose { session.end() }
        }

        val room = requireRoom()
        val localMedia = rememberLocalMedia()
        val agent = rememberAgent()
        val sessionMessages = rememberSessionMessages()
        val controller = remember(session, localMedia, agent) {
            LiveKitHermesSessionController(session, localMedia, agent)
        }
        val chatTransport = remember(sessionMessages, room) {
            LiveKitHermesChatTransport(sessionMessages, room)
        }
        val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
        val statusScope = rememberCoroutineScope()
        var pendingApproval by remember { mutableStateOf<ApprovalRequest?>(null) }
        var statusUpdates by remember { mutableStateOf(emptyList<TimelineUpdate>()) }
        val messageUpdates = remember(sessionMessages.messages) {
            sessionMessages.messages.mapNotNull { message ->
                message.toTimelineUpdate(room)
            }
        }
        val initialHistory = remember(viewModel.identity.conversationId) {
            viewModel.historyRepository.load(viewModel.identity.conversationId)
        }

        LaunchedEffect(room) {
            room.events.events.collect { event ->
                if (event is RoomEvent.DataReceived &&
                    event.topic == APPROVAL_REQUEST_TOPIC
                ) {
                    parseApprovalRequest(event.data)?.let { pendingApproval = it }
                }
            }
        }

        DisposableEffect(room) {
            room.registerTextStreamHandler(STATUS_TOPIC) { receiver, _ ->
                statusScope.launch {
                    val payload = StringBuilder()
                    receiver.flow.collect(payload::append)
                    parseStatusPacket(payload.toString().encodeToByteArray())
                        ?.toTimelineUpdate(
                            eventId = receiver.info.id,
                            timestampMs = receiver.info.timestampMs
                        )
                        ?.let { statusUpdates = statusUpdates + it }
                }
            }
            onDispose { room.unregisterTextStreamHandler(STATUS_TOPIC) }
        }

        fun respondToApproval(choice: String) {
            val request = pendingApproval ?: return
            pendingApproval = null
            val payload = approvalResponseJson(request.runId, choice) ?: return
            coroutineScope.launch {
                room.localParticipant.publishData(
                    payload.encodeToByteArray(),
                    reliability = DataPublishReliability.RELIABLE,
                    topic = APPROVAL_RESPONSE_TOPIC
                )
            }
        }

        Box(modifier = modifier) {
            HermesScreen(
                controller = controller,
                chatTransport = chatTransport,
                incomingUpdates = messageUpdates + statusUpdates,
                initialMessages = initialHistory,
                canEnableMic = canEnableMic,
                onRequestMicrophonePermission = { requestedAudio = true },
                onRotateConversation = {
                    val identity = viewModel.rotateConversation()
                    room.localParticipant.updateAttributes(
                        mapOf("hermes.conversation_id" to identity.conversationId)
                    )
                    identity.conversationId
                },
                onTimelineChanged = {
                    viewModel.historyRepository.save(
                        viewModel.identity.conversationId,
                        it
                    )
                },
                onConnectionError = {
                    Toast.makeText(
                        context,
                        "Hermes-এর সঙ্গে সংযোগ করা যায়নি।",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            pendingApproval?.let { request ->
                HermesApprovalDialog(
                    request = request,
                    onConfirm = { respondToApproval("once") },
                    onCancel = { respondToApproval("deny") }
                )
            }
        }
    }
}

@Composable
fun HermesScreen(
    controller: HermesSessionController,
    modifier: Modifier = Modifier,
    chatTransport: HermesChatTransport = NoOpHermesChatTransport,
    incomingUpdates: List<TimelineUpdate> = emptyList(),
    initialMessages: List<TimelineMessage> = emptyList(),
    canEnableMic: Boolean = true,
    onRequestMicrophonePermission: () -> Unit = {},
    onRotateConversation: () -> String = { "conversation-${UUID.randomUUID()}" },
    onTimelineChanged: (List<TimelineMessage>) -> Unit = {},
    onConnectionError: (Throwable) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var callActive by remember { mutableStateOf(false) }
    var micEnabled by remember { mutableStateOf(false) }
    var pendingCall by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var timeline by remember { mutableStateOf(initialMessages) }
    var working by remember { mutableStateOf(false) }
    var activeLatency by remember { mutableStateOf<LatencyTracker?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            controller.start(microphoneEnabled = false)
            controller.setAgentVolume(0.0)
        }.onFailure(onConnectionError)
    }

    LaunchedEffect(incomingUpdates) {
        var next = timeline
        incomingUpdates.forEach { update ->
            next = reduceTimeline(next, update)
            if (update is TimelineUpdate.Status) {
                working = update.statusType in setOf(
                    "delegation.requested",
                    "tool.started",
                    "subagent.start"
                )
                if (update.statusType in setOf(
                        "run.completed",
                        "run.failed",
                        "run.cancelled"
                    )
                ) {
                    working = false
                }
            }
            if (update is TimelineUpdate.Transcript && !update.isUser) {
                activeLatency?.let { tracker ->
                    if (tracker.durationMs("send_pressed", "first_ui_delta") == null) {
                        tracker.mark("first_ui_delta")
                    }
                }
                if (update.isFinal) working = false
            }
        }
        timeline = next
    }

    LaunchedEffect(timeline) {
        onTimelineChanged(timeline)
    }

    LaunchedEffect(canEnableMic, pendingCall) {
        if (pendingCall && canEnableMic) {
            controller.setAgentVolume(1.0)
            controller.setMicrophoneEnabled(true)
            callActive = true
            micEnabled = true
            pendingCall = false
        }
    }

    fun beginCall() {
        if (!canEnableMic) {
            pendingCall = true
            onRequestMicrophonePermission()
            return
        }
        coroutineScope.launch {
            controller.setAgentVolume(1.0)
            controller.setMicrophoneEnabled(true)
            callActive = true
            micEnabled = true
        }
    }

    fun mute() {
        coroutineScope.launch {
            controller.setMicrophoneEnabled(false)
            micEnabled = false
        }
    }

    fun unmute() {
        if (!callActive) {
            beginCall()
            return
        }
        if (!canEnableMic) {
            pendingCall = true
            onRequestMicrophonePermission()
            return
        }
        coroutineScope.launch {
            controller.setMicrophoneEnabled(true)
            micEnabled = true
        }
    }

    fun endCall() {
        coroutineScope.launch {
            controller.setMicrophoneEnabled(false)
            controller.setAgentVolume(0.0)
            callActive = false
            micEnabled = false
        }
    }

    fun addStatus(text: String, type: String, operationId: String = newOperationId()) {
        timeline = reduceTimeline(
            timeline,
            TimelineUpdate.Status(operationId, text, type, System.currentTimeMillis())
        )
    }

    fun sendControl(command: HermesCommand, conversationId: String? = null) {
        val operationId = newOperationId()
        coroutineScope.launch {
            if (!chatTransport.sendControl(
                    ControlPacket(
                        opId = operationId,
                        command = command.wireValue,
                        conversationId = conversationId
                    )
                )
            ) {
                addStatus("Control delivery failed", "delivery.failed", operationId)
            }
        }
    }

    fun sendMessage(text: String) {
        val localId = newOperationId()
        val operationId = newOperationId()
        val tracker = LatencyTracker(operationId).apply { mark("send_pressed") }
        activeLatency = tracker
        timeline = reduceTimeline(
            timeline,
            TimelineUpdate.LocalText(localId, text, System.currentTimeMillis())
        )
        working = true
        coroutineScope.launch {
            runCatching {
                chatTransport.sendMessage(text, localId, operationId)
            }.onSuccess { transportId ->
                tracker.mark("packet_sent")
                timeline = reduceTimeline(
                    timeline,
                    TimelineUpdate.TextSent(
                        localId,
                        transportId,
                        System.currentTimeMillis()
                    )
                )
            }.onFailure {
                working = false
                timeline = reduceTimeline(
                    timeline,
                    TimelineUpdate.TextFailed(localId, System.currentTimeMillis())
                )
            }
        }
    }

    fun submit(raw: String) {
        if (raw.isBlank()) return
        input = ""
        when (val intent = parseInput(raw.trim())) {
            is InputIntent.Message -> sendMessage(intent.text)
            is InputIntent.Control -> when (intent.command) {
                HermesCommand.NEW -> {
                    val conversationId = onRotateConversation()
                    timeline = emptyList()
                    working = false
                    sendControl(HermesCommand.NEW, conversationId)
                }
                HermesCommand.STATUS -> {
                    addStatus(
                        if (controller.isConnected) "Connected · text ready" else "Connecting",
                        "local.status"
                    )
                    sendControl(HermesCommand.STATUS)
                }
                HermesCommand.STOP -> sendControl(HermesCommand.STOP)
                else -> Unit
            }
            is InputIntent.Local -> when (intent.command) {
                HermesCommand.MUTE -> mute()
                HermesCommand.UNMUTE -> unmute()
                HermesCommand.VOICE,
                HermesCommand.CALL -> beginCall()
                HermesCommand.ENDCALL -> endCall()
                HermesCommand.HELP -> addStatus(
                    "Use text, @agent routes, or /new /status /stop /call /mute",
                    "local.help"
                )
                else -> Unit
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "HERMES",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = when {
                        controller.isReconnecting -> "Reconnecting..."
                        controller.isConnected -> "Connected"
                        else -> "Connecting..."
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        ChatLog(
            messages = timeline,
            working = working,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 10.dp)
                .testTag("conversation_timeline")
        )

        Text(
            text = if (micEnabled) "মাইক্রোফোন চালু" else "মাইক্রোফোন বন্ধ",
            modifier = Modifier.padding(bottom = 6.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!callActive) {
                OutlinedButton(onClick = ::beginCall) {
                    Icon(Icons.Default.Call, contentDescription = "Call Hermes")
                    Spacer(Modifier.size(6.dp))
                    Text("CALL HERMES")
                }
            } else {
                OutlinedButton(onClick = { if (micEnabled) mute() else unmute() }) {
                    Icon(
                        if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Toggle microphone"
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(if (micEnabled) "MUTE" else "UNMUTE")
                }
                Button(
                    onClick = ::endCall,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "End call")
                    Spacer(Modifier.size(6.dp))
                    Text("END CALL")
                }
            }
        }

        ChatBar(
            value = input,
            onValueChange = { input = it },
            onChatSend = ::submit,
            suggestions = suggestInputs(input),
            onSuggestionSelected = { input = "$it " },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}

private fun ReceivedMessage.toTimelineUpdate(room: Room): TimelineUpdate = when (this) {
    is ReceivedUserTranscriptionMessage -> TimelineUpdate.Transcript(
        segmentId = attributes["lk.segment_id"] ?: id,
        text = message,
        isFinal = attributes["lk.transcription_final"] == "true",
        isUser = true,
        timestampMs = timestamp
    )
    is ReceivedAgentTranscriptionMessage -> TimelineUpdate.Transcript(
        segmentId = attributes["lk.segment_id"] ?: id,
        text = message,
        isFinal = attributes["lk.transcription_final"] == "true",
        isUser = false,
        timestampMs = timestamp
    )
    is ReceivedChatMessage -> TimelineUpdate.RemoteText(
        transportId = id,
        text = message,
        isFinal = true,
        timestampMs = timestamp,
        isUser = fromParticipant?.identity == room.localParticipant.identity,
        localId = attributes["local_id"]
    )
}

private fun StatusPacket.toTimelineUpdate(
    eventId: String,
    timestampMs: Long
): TimelineUpdate? {
    val text = when (type) {
        "session.ready" -> "Hermes session ready"
        "tool.started" -> tool?.let { "Using $it" }
        "tool.completed" -> tool?.let { "$it completed" }
        "subagent.start" -> status ?: "Specialist working"
        "subagent.complete" -> status ?: "Specialist completed"
        "delegation.requested" -> status ?: mention?.let { "$it assigned" }
        "approval.request" -> "Confirmation required on this device"
        "run.completed" -> "Completed"
        "run.failed" -> "Hermes run failed"
        "run.cancelled" -> "Stopped"
        "first_hermes_delta" -> durationMs?.let { "First response · ${it}ms" }
        else -> null
    } ?: return null
    return TimelineUpdate.Status(eventId, text, type, timestampMs)
}

private fun newOperationId(): String = "op-${UUID.randomUUID()}"

@Composable
fun HermesApprovalDialog(
    request: ApprovalRequest,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                "⚠ DESTRUCTIVE ACTION",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Agent: ${request.displayAgent}")
                Text("Action: ${request.action}")
                Text("Target: ${request.target}")
                Text("Reason: ${request.reason}")
                Text(
                    "Voice or text approval is not accepted. Confirm only by tapping this dialog.",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("CONFIRM") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("CANCEL") }
        }
    )
}
