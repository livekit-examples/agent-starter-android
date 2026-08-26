package io.livekit.android.example.voiceassistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApprovalProtocolTest {
    @Test
    fun parsesApprovalRequest() {
        val request = parseApprovalRequest(
            """{"runId":"run_1","target":"C:\\\\safe-test.txt","action":"Delete file","reason":"Requested by user"}"""
                .encodeToByteArray()
        )

        assertEquals("run_1", request?.runId)
        assertEquals("Delete file", request?.action)
    }

    @Test
    fun rejectsMalformedOrIncompleteRequest() {
        assertNull(parseApprovalRequest("not json".encodeToByteArray()))
        assertNull(parseApprovalRequest("""{"runId":"run_1"}""".encodeToByteArray()))
    }

    @Test
    fun responseAllowsOnlyOneShotConfirmOrDeny() {
        assertEquals(
            """{"runId":"run_1","choice":"once"}""",
            approvalResponseJson("run_1", "once")
        )
        assertEquals(
            """{"runId":"run_1","choice":"deny"}""",
            approvalResponseJson("run_1", "deny")
        )
        assertNull(approvalResponseJson("run_1", "always"))
        assertNull(approvalResponseJson("run_1", "yes"))
    }

    @Test
    fun approveAliasesRemainRejected() {
        assertNull(approvalResponseJson("run-1", "approve"))
        assertNull(approvalResponseJson("run-1", "yes"))
        assertNull(approvalResponseJson("run-1", "হ্যাঁ"))
    }

    @Test
    fun optionalAgentDefaultsToHermesMain() {
        val explicit = parseApprovalRequest(
            """{"runId":"run-1","target":"fixture","action":"Delete","reason":"test","agent":"Computer Operator"}"""
                .encodeToByteArray()
        )
        val missing = parseApprovalRequest(
            """{"runId":"run-2","target":"fixture","action":"Delete","reason":"test"}"""
                .encodeToByteArray()
        )

        assertEquals("Computer Operator", explicit?.displayAgent)
        assertEquals("Hermes Main", missing?.displayAgent)
    }
}
