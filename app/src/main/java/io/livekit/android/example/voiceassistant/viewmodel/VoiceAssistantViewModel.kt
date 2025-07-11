package io.livekit.android.example.voiceassistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import io.livekit.android.LiveKit
import io.livekit.android.audio.withPreconnectAudio
import io.livekit.android.example.voiceassistant.screen.VoiceAssistantRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * This ViewModel handles holding onto the Room object, so that it is
 * maintained across configuration changes, such as rotation.
 */
class VoiceAssistantViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    val room = LiveKit.create(application)

    init {
        val args = savedStateHandle.toRoute<VoiceAssistantRoute>()
        viewModelScope.launch(Dispatchers.IO) {

            // Use preconnect audio to capture audio while connecting for faster user-perceived connection times.
            room.withPreconnectAudio {
                room.connect(args.url, args.token)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        room.disconnect()
        room.release()
    }
}