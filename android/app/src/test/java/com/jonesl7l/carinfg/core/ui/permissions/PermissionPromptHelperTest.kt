package com.jonesl7l.carinfg.core.ui.permissions

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class PermissionPromptHelperTest {
    @Test
    fun `a first request goes straight to the system prompt`() {
        val prompt = PermissionPromptHelper.promptFor(
            showRationale = false,
            justDenied = false,
        )

        assertThat(prompt).isEqualTo(PermissionPrompt.NONE)
    }

    @Test
    fun `a request after one denial shows our explanation`() {
        val prompt = PermissionPromptHelper.promptFor(
            showRationale = true,
            justDenied = false,
        )

        assertThat(prompt).isEqualTo(PermissionPrompt.RATIONALE)
    }

    @Test
    fun `a first denial shows our explanation`() {
        val prompt = PermissionPromptHelper.promptFor(
            showRationale = true,
            justDenied = true,
        )

        assertThat(prompt).isEqualTo(PermissionPrompt.RATIONALE)
    }

    @Test
    fun `a denial with no rationale due is permanent and points to settings`() {
        val prompt = PermissionPromptHelper.promptFor(
            showRationale = false,
            justDenied = true,
        )

        assertThat(prompt).isEqualTo(PermissionPrompt.SETTINGS)
    }
}
