package com.jonesl7l.carinfg.core.network.result

enum class ErrorCategory {
    /** Sync tries again later. */
    RETRY,

    /** Shown to the user to act on. */
    NEEDS_USER,

    /** Signs the user out. */
    SESSION,

    /** Client and server disagree; logged as an error. */
    BUG,
}
