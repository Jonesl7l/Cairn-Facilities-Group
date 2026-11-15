package com.jonesl7l.carinfg.core.domain.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jonesl7l.carinfg.core.domain.model.incident.Severity
import org.junit.Test

class SeverityTest {
    @Test
    fun `rank orders critical first and unknown last`() {
        val severities =
            listOf(
                Severity.Unknown(raw = "SEVERE"),
                Severity.Medium,
                Severity.Critical,
                Severity.Low,
                Severity.High,
            )

        val expected =
            listOf(
                Severity.Critical,
                Severity.High,
                Severity.Medium,
                Severity.Low,
                Severity.Unknown(raw = "SEVERE"),
            )
        assertThat(severities.sortedByDescending { it.rank }).isEqualTo(expected)
    }
}
