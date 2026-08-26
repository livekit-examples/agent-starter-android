package io.livekit.android.example.voiceassistant

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import io.livekit.android.example.voiceassistant.screen.HermesScreen
import io.livekit.android.example.voiceassistant.screen.HermesSessionController
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HermesLifecycleTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun launchShowsChatAndCallWithMicOff() {
        val controller = FakeHermesSessionController()

        composeRule.setContent { HermesScreen(controller) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("conversation_timeline").assertExists()
        composeRule.onNodeWithText("CALL HERMES").assertExists()
        composeRule.onNodeWithText("মাইক্রোফোন বন্ধ").assertExists()
        assertEquals(1, controller.startCalls)
        assertEquals(false, controller.startMicrophoneEnabled)
        assertEquals(0, controller.enableMicCalls)
    }

    private class FakeHermesSessionController : HermesSessionController {
        var startCalls = 0
        var startMicrophoneEnabled: Boolean? = null
        var enableMicCalls = 0

        override val isConnected = false
        override val isReconnecting = false

        override suspend fun start(microphoneEnabled: Boolean) {
            startCalls += 1
            startMicrophoneEnabled = microphoneEnabled
        }

        override suspend fun setMicrophoneEnabled(enabled: Boolean) {
            if (enabled) enableMicCalls += 1
        }

        override suspend fun setAgentVolume(volume: Double) = Unit
    }
}
