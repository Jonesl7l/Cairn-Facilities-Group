package com.jonesl7l.carinfg.feature.account

data class AccountUiState(
    val displayName: String = "",
    val username: String = "",
    val initials: String = "",
    val unsyncedCount: Int = 0,
    val isLoggingOut: Boolean = false,
)
