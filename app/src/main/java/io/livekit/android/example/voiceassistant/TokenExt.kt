package io.livekit.android.example.voiceassistant

import androidx.annotation.Keep
import com.github.ajalt.timberkt.Timber
import com.google.gson.Gson
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// TODO: Add your Sandbox ID here
private const val sandboxID = ""

// NOTE: If you prefer not to use LiveKit Sandboxes for testing, you can generate your
// tokens manually by visiting https://cloud.livekit.io/projects/p_/settings/keys
// and using one of your API Keys to generate a token with custom TTL and permissions.
private const val hardcodedUrl = ""
private const val hardcodedToken = ""

private val okHttpClient by lazy { OkHttpClient() }

/**
 * Retrieves a LiveKit from the token server.
 *
 * Currently configured to use LiveKit's Sandbox token server.
 * When building an app for production, you should use your own token server.
 */
suspend fun retrieveToken(): ConnectionDetails = suspendCancellableCoroutine { continuation ->
    if (sandboxID.isEmpty()) {
        Timber.w { "sandboxID not populated, using default URL and token." }
        continuation.resume(ConnectionDetails(hardcodedUrl, hardcodedToken))
        return@suspendCancellableCoroutine
    }

    val tokenEndpoint = "https://cloud-api.livekit.io/api/sandbox/connection-details"

    val request = Request.Builder()
        .url(tokenEndpoint)
        .header("X-Sandbox-ID", sandboxID)
        .build()

    okHttpClient.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.body.let { responseBody ->
                if (response.isSuccessful) {
                    val json = responseBody.string()
                    val cd = Gson().fromJson(json, ConnectionDetails::class.java)
                    continuation.resume(cd)
                } else {
                    continuation.resumeWithException(
                        Exception("Failed to get connection details with response code ${response.code}: ${response.message})")
                    )
                }
            }
        }
    })
}

@Keep
data class ConnectionDetails(
    val serverUrl: String,
    val participantToken: String,
)