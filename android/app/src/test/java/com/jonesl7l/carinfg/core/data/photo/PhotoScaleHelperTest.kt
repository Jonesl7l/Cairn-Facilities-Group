package com.jonesl7l.carinfg.core.data.photo

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class PhotoScaleHelperTest {
    @Test
    fun `the sample size halves until the next halving would drop below the limit`() {
        assertThat(sampleSize(width = 4000, height = 3000)).isEqualTo(1)
        assertThat(sampleSize(width = 8000, height = 6000)).isEqualTo(2)
        assertThat(sampleSize(width = 3000, height = 9000)).isEqualTo(4)
    }

    @Test
    fun `small photos are not sampled or scaled`() {
        assertThat(sampleSize(width = 1200, height = 900)).isEqualTo(1)
        assertThat(scale(width = 1200, height = 900)).isEqualTo(1f)
    }

    @Test
    fun `the scale brings the long edge down to the limit`() {
        assertThat(scale(width = 4096, height = 3072)).isEqualTo(0.5f)
        assertThat(scale(width = 1000, height = 4096)).isEqualTo(0.5f)
    }

    private fun sampleSize(
        width: Int,
        height: Int,
    ) = PhotoScaleHelper.sampleSize(
        width = width,
        height = height,
        maxEdge = MAX_EDGE,
    )

    private fun scale(
        width: Int,
        height: Int,
    ) = PhotoScaleHelper.scale(
        width = width,
        height = height,
        maxEdge = MAX_EDGE,
    )

    private companion object {
        const val MAX_EDGE = 2048
    }
}
