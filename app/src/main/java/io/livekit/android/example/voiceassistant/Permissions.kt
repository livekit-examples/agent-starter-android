package io.livekit.android.example.voiceassistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Handles requesting the required permissions if needed.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun requirePermissions(microphone: Boolean, camera: Boolean): MultiplePermissionsState {
    val permissionsState = rememberMultiplePermissionsState(
        listOfNotNull(
            if (microphone) android.Manifest.permission.RECORD_AUDIO else null,
            if (camera) android.Manifest.permission.CAMERA else null,
        )
    )

    DisposableEffect(camera, microphone) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
        onDispose { /* do nothing */ }
    }
    return permissionsState
}

/**
 * @return true if the camera permission is granted.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberCanEnableCamera(): State<Boolean> {
    val permissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )
    return remember {
        derivedStateOf {
            permissionState.status.isGranted
        }
    }
}

/**
 * @return true if both enabled is true and the mic permission is granted.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberCanEnableMic(): State<Boolean> {
    val micPermissionState = rememberPermissionState(
        android.Manifest.permission.RECORD_AUDIO
    )
    return remember(micPermissionState) {
        derivedStateOf {
            micPermissionState.status.isGranted
        }
    }
}
