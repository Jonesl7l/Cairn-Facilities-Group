package com.jonesl7l.carinfg.core.ui.permissions

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jonesl7l.carinfg.R

@Composable
fun PermissionDialogs(
    state: PermissionState,
    title: String,
    rationale: String,
    settingsRationale: String,
) {
    when (state.prompt) {
        PermissionPrompt.NONE -> Unit

        PermissionPrompt.RATIONALE -> PermissionDialog(
            title = title,
            text = rationale,
            confirmLabel = stringResource(id = R.string.permission_retry),
            onConfirm = state.retry,
            onDismiss = state.dismiss,
        )

        PermissionPrompt.SETTINGS -> PermissionDialog(
            title = title,
            text = settingsRationale,
            confirmLabel = stringResource(id = R.string.permission_open_settings),
            onConfirm = state.openSettings,
            onDismiss = state.dismiss,
        )
    }
}

@Composable
private fun PermissionDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Text(
                text = text,
                modifier = Modifier.verticalScroll(state = rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(text = confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(id = R.string.permission_not_now)) }
        },
    )
}
