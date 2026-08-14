package io.livekit.android.example.voiceassistant.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import io.livekit.android.LiveKit
import io.livekit.android.example.voiceassistant.screen.VoiceAssistantRoute
import io.livekit.android.token.TokenSource
import java.net.URI

/**
 * This ViewModel handles holding onto the Room object, so that it is
 * maintained across configuration changes, such as rotation.
 */
class VoiceAssistantViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    val room = LiveKit.create(application)

    val tokenSource: TokenSource

    init {
        val (tokenServerId, url, token, homepageAgentEndpoint) = savedStateHandle.toRoute<VoiceAssistantRoute>()

        tokenSource = if (tokenServerId.isNotEmpty()) {
            TokenSource.fromDevelopmentTokenServer(tokenServerId = tokenServerId)
        } else if (url.isNotEmpty() && token.isNotEmpty()) {
            TokenSource.fromLiteral(url, token)
        } else {
            if (url.isNotEmpty() || token.isNotEmpty()) {
                Log.w(TAG, "hardcodedUrl and hardcodedToken must both be set; falling back to the homepage agent.")
            }
            TokenSource.fromEndpoint(URI(homepageAgentEndpoint).toURL())
        }
    }

    override fun onCleared() {
        super.onCleared()
        room.disconnect()
        room.release()
    }

    companion object {
        private const val TAG = "VoiceAssistantViewModel"
    }
}