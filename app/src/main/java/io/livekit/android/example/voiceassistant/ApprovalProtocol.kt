package io.livekit.android.example.voiceassistant

import com.google.gson.Gson

const val APPROVAL_REQUEST_TOPIC = "hermes.approval.request"
const val APPROVAL_RESPONSE_TOPIC = "hermes.approval.response"

data class ApprovalRequest(
    val runId: String,
    val target: String,
    val action: String,
    val reason: String,
    val agent: String? = null
) {
    val displayAgent: String
        get() = agent?.takeIf(String::isNotBlank) ?: "Hermes Main"
}

private data class ApprovalResponse(val runId: String, val choice: String)

private val approvalGson = Gson()

fun parseApprovalRequest(data: ByteArray): ApprovalRequest? {
    if (data.isEmpty() || data.size > 4096) return null
    val request = runCatching {
        approvalGson.fromJson(data.decodeToString(), ApprovalRequest::class.java)
    }.getOrNull() ?: return null

    return runCatching {
        request.takeIf {
            it.runId.isNotBlank() &&
                it.target.isNotBlank() &&
                it.action.isNotBlank() &&
                it.reason.isNotBlank()
        }
    }.getOrNull()?.copy(
        agent = request.agent
            ?.trim()
            ?.takeIf { it.length <= 80 && it.none(Char::isISOControl) }
    )
}

fun approvalResponseJson(runId: String, choice: String): String? {
    if (runId.isBlank() || choice !in setOf("once", "deny")) return null
    return approvalGson.toJson(ApprovalResponse(runId, choice))
}
