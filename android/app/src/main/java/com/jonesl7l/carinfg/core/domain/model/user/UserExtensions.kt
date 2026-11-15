package com.jonesl7l.carinfg.core.domain.model.user

private const val MAX_INITIALS = 2

fun User.initials(): String = displayName
    .split(" ")
    .filter { it.isNotBlank() }
    .take(MAX_INITIALS)
    .joinToString(separator = "") { it.first().uppercase() }
