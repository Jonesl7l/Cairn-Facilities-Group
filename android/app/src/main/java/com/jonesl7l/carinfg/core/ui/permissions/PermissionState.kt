package com.jonesl7l.carinfg.core.ui.permissions

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

class PermissionState(
    val prompt: PermissionPrompt,
    val request: () -> Unit,
    val retry: () -> Unit,
    val openSettings: () -> Unit,
    val dismiss: () -> Unit,
)

@Composable
fun rememberPermissionState(
    permission: String,
    onGrant: () -> Unit,
): PermissionState {
    val activity = checkNotNull(LocalActivity.current)
    val currentOnGrant by rememberUpdatedState(onGrant)
    var prompt by rememberSaveable { mutableStateOf(PermissionPrompt.NONE) }
    var isInSettings by rememberSaveable { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                prompt = PermissionPrompt.NONE
                currentOnGrant()
            } else {
                prompt =
                    PermissionPromptHelper.promptFor(
                        showRationale = activity.showsRationale(permission = permission),
                        justDenied = true,
                    )
            }
        }
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        if (!isInSettings) return@LifecycleEventEffect
        isInSettings = false
        if (activity.isGranted(permission = permission)) currentOnGrant()
    }
    return PermissionState(
        prompt = prompt,
        request = {
            if (activity.isGranted(permission = permission)) {
                currentOnGrant()
            } else {
                prompt =
                    PermissionPromptHelper.promptFor(
                        showRationale = activity.showsRationale(permission = permission),
                        justDenied = false,
                    )
                if (prompt == PermissionPrompt.NONE) launcher.launch(permission)
            }
        },
        retry = {
            prompt = PermissionPrompt.NONE
            launcher.launch(permission)
        },
        openSettings = {
            prompt = PermissionPrompt.NONE
            isInSettings = true
            activity.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts(
                        PACKAGE_SCHEME,
                        activity.packageName,
                        null,
                    ),
                ),
            )
        },
        dismiss = { prompt = PermissionPrompt.NONE },
    )
}

private fun Activity.isGranted(permission: String): Boolean = ContextCompat.checkSelfPermission(
    this,
    permission,
) == PackageManager.PERMISSION_GRANTED

private fun Activity.showsRationale(permission: String): Boolean = ActivityCompat.shouldShowRequestPermissionRationale(
    this,
    permission,
)

private const val PACKAGE_SCHEME = "package"
