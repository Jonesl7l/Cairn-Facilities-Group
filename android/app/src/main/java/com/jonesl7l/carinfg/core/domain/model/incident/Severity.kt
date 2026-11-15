package com.jonesl7l.carinfg.core.domain.model.incident

sealed interface Severity {
    val rank: Int

    data object Low : Severity {
        override val rank = 1
    }

    data object Medium : Severity {
        override val rank = 2
    }

    data object High : Severity {
        override val rank = 3
    }

    data object Critical : Severity {
        override val rank = 4
    }

    /** server value this app version doesnt know yet */
    data class Unknown(val raw: String) : Severity {
        override val rank = 0
    }
}
