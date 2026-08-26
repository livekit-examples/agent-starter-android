package io.livekit.android.example.voiceassistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.livekit.android.LiveKit
import io.livekit.android.example.voiceassistant.hardcodedToken
import io.livekit.android.example.voiceassistant.hardcodedUrl
import io.livekit.android.example.voiceassistant.realtime.HistoryRepository
import io.livekit.android.example.voiceassistant.realtime.SessionIdentity
import io.livekit.android.example.voiceassistant.realtime.SessionIdentityStore
import io.livekit.android.example.voiceassistant.realtime.SharedPreferencesHistoryStorage
import io.livekit.android.example.voiceassistant.realtime.SharedPreferencesIdentityStorage
import io.livekit.android.example.voiceassistant.tokenServerId
import io.livekit.android.token.TokenRequestOptions
import io.livekit.android.token.TokenSource
import io.livekit.android.token.cached

/**
 * This ViewModel handles holding onto the Room object, so that it is
 * maintained across configuration changes, such as rotation.
 */
class VoiceAssistantViewModel(application: Application) : AndroidViewModel(application) {

    val room = LiveKit.create(application)

    val tokenSource: TokenSource
    private val preferences = application.getSharedPreferences(
        "hermes_private_state",
        Application.MODE_PRIVATE
    )
    private val identityStore = SessionIdentityStore(
        SharedPreferencesIdentityStorage(preferences)
    )
    val historyRepository = HistoryRepository(
        SharedPreferencesHistoryStorage(preferences)
    )

    var identity: SessionIdentity = identityStore.current()
        private set

    init {
        tokenSource = if (tokenServerId.isNotEmpty()) {
            TokenSource.fromDevelopmentTokenServer(tokenServerId = tokenServerId)
                .cached()
        } else if (hardcodedUrl.isNotEmpty() && hardcodedToken.isNotEmpty()) {
            TokenSource.fromLiteral(hardcodedUrl, hardcodedToken)
                .cached()
        } else {
            error("A LiveKit development token server ID or literal URL/token pair is required")
        }
    }

    fun tokenRequestOptions(agentName: String): TokenRequestOptions = TokenRequestOptions(
        agentName = agentName,
        participantIdentity = "hermes-android-${identity.installationId}",
        participantAttributes = mapOf(
            "hermes.conversation_id" to identity.conversationId
        )
    )

    fun rotateConversation(): SessionIdentity {
        identity = identityStore.rotateConversation()
        return identity
    }

    override fun onCleared() {
        super.onCleared()
        room.disconnect()
        room.release()
    }
}
