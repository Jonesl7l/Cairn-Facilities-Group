package com.jonesl7l.carinfg.core.ui.permissions

object PermissionPromptHelper {
    fun promptFor(
        showRationale: Boolean,
        justDenied: Boolean,
    ): PermissionPrompt = when {
        showRationale -> PermissionPrompt.RATIONALE
        justDenied -> PermissionPrompt.SETTINGS
        else -> PermissionPrompt.NONE
    }
}
