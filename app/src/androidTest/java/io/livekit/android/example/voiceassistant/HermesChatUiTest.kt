package io.livekit.android.example.voiceassistant

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.livekit.android.example.voiceassistant.realtime.ControlPacket
import io.livekit.android.example.voiceassistant.screen.HermesChatTransport
import io.livekit.android.example.voiceassistant.screen.HermesScreen
import io.livekit.android.example.voiceassistant.screen.HermesSessionController
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class HermesChatUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun sendAddsBubbleBeforeTransportCompletes() {
        val transport = FakeChatTransport(blockSend = true)
        composeRule.setContent {
            HermesScreen(FakeVoiceController(), chatTransport = transport)
        }

        submit("hello")

        composeRule.onNodeWithText("hello").assertExists()
        assertFalse(transport.completion.isCompleted)
        assertEquals(1, transport.chatSends)
    }

    @Test
    fun slashAndMentionSuggestionsAppearImmediately() {
        composeRule.setContent {
            HermesScreen(FakeVoiceController(), chatTransport = FakeChatTransport())
        }

        composeRule.onNodeWithTag("message_input").performTextInput("@c")
        composeRule.onNodeWithText("@coder").assertExists()
        composeRule.onNodeWithText("@computer-operator").assertExists()
        composeRule.onNodeWithTag("message_input").performTextClearance()
        composeRule.onNodeWithTag("message_input").performTextInput("/m")
        composeRule.onNodeWithText("/mute").assertExists()
        composeRule.onNodeWithText("/memory").assertExists()
    }

    @Test
    fun muteAndEndCallDoNotSendChat() {
        val transport = FakeChatTransport()
        val voice = FakeVoiceController()
        composeRule.setContent { HermesScreen(voice, chatTransport = transport) }

        submit("/mute")
        submit("/endcall")
        composeRule.waitForIdle()

        assertEquals(0, transport.chatSends)
        assertEquals(listOf(false, false), voice.micStates)
    }

    private fun submit(text: String) {
        composeRule.onNodeWithTag("message_input").performTextInput(text)
        composeRule.onNodeWithTag("send_button").performClick()
        composeRule.waitForIdle()
    }

    private class FakeChatTransport(
        private val blockSend: Boolean = false
    ) : HermesChatTransport {
        var chatSends = 0
        val completion = CompletableDeferred<Unit>()

        override suspend fun sendMessage(
            text: String,
            localId: String,
            operationId: String
        ): String {
            chatSends += 1
            if (blockSend) completion.await()
            return "stream-$chatSends"
        }

        override suspend fun sendControl(packet: ControlPacket): Boolean = true
    }

    private class FakeVoiceController : HermesSessionController {
        val micStates = mutableListOf<Boolean>()
        override val isConnected = true
        override val isReconnecting = false

        override suspend fun start(microphoneEnabled: Boolean) = Unit

        override suspend fun setMicrophoneEnabled(enabled: Boolean) {
            micStates += enabled
        }

        override suspend fun setAgentVolume(volume: Double) = Unit
    }
}
