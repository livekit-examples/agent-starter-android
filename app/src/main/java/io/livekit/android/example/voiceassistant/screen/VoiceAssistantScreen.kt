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
import io.livekit.android.compose.state.SessionOptions
import io.livekit.android.compose.state.rememberAgent
import io.livekit.android.compose.state.rememberLocalMedia
import io.livekit.android.compose.state.rememberSession
import io.livekit.android.compose.types.LocalMedia
import io.livekit.android.events.RoomEvent
import io.livekit.android.example.voiceassistant.APPROVAL_REQUEST_TOPIC
import io.livekit.android.example.voiceassistant.APPROVAL_RESPONSE_TOPIC
import io.livekit.android.example.voiceassistant.ApprovalRequest
import io.livekit.android.example.voiceassistant.approvalResponseJson
import io.livekit.android.example.voiceassistant.parseApprovalRequest
import io.livekit.android.example.voiceassistant.rememberCanEnableMic
import io.livekit.android.example.voiceassistant.requirePermissions
import io.livekit.android.example.voiceassistant.viewmodel.VoiceAssistantViewModel
import io.livekit.android.room.track.DataPublishReliability
import io.livekit.android.room.track.RemoteAudioTrack
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

    suspend fun start()

    suspend fun setMicrophoneEnabled(enabled: Boolean)

    suspend fun setAgentVolume(volume: Double)
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

    override suspend fun start() {
        session.start().getOrThrow()
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
        val controller = remember(session, localMedia, agent) {
            LiveKitHermesSessionController(session, localMedia, agent)
        }
        val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
        var pendingApproval by remember { mutableStateOf<ApprovalRequest?>(null) }

        LaunchedEffect(room) {
            room.events.events.collect { event ->
                if (event is RoomEvent.DataReceived &&
                    event.topic == APPROVAL_REQUEST_TOPIC
                ) {
                    parseApprovalRequest(event.data)?.let { pendingApproval = it }
                }
            }
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
                canEnableMic = canEnableMic,
                onRequestMicrophonePermission = { requestedAudio = true },
                onConnectionError = {
                    Toast.makeText(
                        context,
                        "Hermes-এর সঙ্গে সংযোগ করা যায়নি।",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            pendingApproval?.let { request ->
                ApprovalDialog(
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
    canEnableMic: Boolean = true,
    onRequestMicrophonePermission: () -> Unit = {},
    onConnectionError: (Throwable) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var callActive by remember { mutableStateOf(false) }
    var micEnabled by remember { mutableStateOf(false) }
    var pendingCall by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            controller.start()
            controller.setAgentVolume(0.0)
        }.onFailure(onConnectionError)
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

    fun endCall() {
        coroutineScope.launch {
            controller.setMicrophoneEnabled(false)
            controller.setAgentVolume(0.0)
            callActive = false
            micEnabled = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HERMES",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.size(8.dp))
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
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp)
                .testTag("conversation_timeline"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Hermes Main conversation",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = if (micEnabled) "মাইক্রোফোন চালু" else "মাইক্রোফোন বন্ধ",
            modifier = Modifier.padding(bottom = 12.dp),
            textAlign = TextAlign.Center
        )

        if (!callActive) {
            Button(
                onClick = ::beginCall,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call Hermes")
                Spacer(Modifier.size(8.dp))
                Text("CALL HERMES")
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            val next = !micEnabled
                            controller.setMicrophoneEnabled(next)
                            micEnabled = next
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Toggle microphone"
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(if (micEnabled) "MUTE" else "UNMUTE")
                }
                Button(
                    onClick = ::endCall,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "End call")
                    Spacer(Modifier.size(8.dp))
                    Text("END CALL")
                }
            }
        }
    }
}

@Composable
private fun ApprovalDialog(
    request: ApprovalRequest,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("অনুমোদন প্রয়োজন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("কাজ", fontWeight = FontWeight.Bold)
                Text(request.action)
                Text("লক্ষ্য", fontWeight = FontWeight.Bold)
                Text(request.target)
                Text("কারণ", fontWeight = FontWeight.Bold)
                Text(request.reason)
                Text(
                    "কণ্ঠে ‘হ্যাঁ’ বললে অনুমোদন হবে না।",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("CONFIRM ONCE") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("CANCEL") }
        }
    )
}
