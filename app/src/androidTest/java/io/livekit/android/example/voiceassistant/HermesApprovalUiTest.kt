package io.livekit.android.example.voiceassistant

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.livekit.android.example.voiceassistant.screen.HermesApprovalDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HermesApprovalUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun destructiveDialogHasOnlyPhysicalConfirmAndCancel() {
        var choice: String? = null
        composeRule.setContent {
            HermesApprovalDialog(
                request = ApprovalRequest(
                    runId = "run-1",
                    target = "fixture",
                    action = "Delete",
                    reason = "Requested for a no-op test",
                    agent = "Computer Operator"
                ),
                onConfirm = { choice = "once" },
                onCancel = { choice = "deny" }
            )
        }

        composeRule.onNodeWithText("⚠ DESTRUCTIVE ACTION").assertExists()
        composeRule.onNodeWithText("Agent: Computer Operator").assertExists()
        composeRule.onNodeWithText("Action: Delete").assertExists()
        composeRule.onNodeWithText("Target: fixture").assertExists()
        composeRule.onNodeWithText("CANCEL").assertExists()
        composeRule.onNodeWithText("CONFIRM").assertExists().performClick()
        composeRule.onNodeWithText("APPROVE").assertDoesNotExist()
        assertEquals("once", choice)
    }
}
