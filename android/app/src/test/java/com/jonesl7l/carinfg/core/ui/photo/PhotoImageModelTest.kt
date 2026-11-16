package com.jonesl7l.carinfg.core.ui.photo

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.jonesl7l.carinfg.BuildConfig
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.photo
import java.io.File
import org.junit.Test

class PhotoImageModelTest {
    @Test
    fun `a photo on the device loads from its file`() {
        val model = photo(localPath = "/data/photo.jpg").imageModel()

        assertThat(model).isEqualTo(File("/data/photo.jpg"))
    }

    @Test
    fun `a photo only on the server loads from the base url`() {
        val model = photo(remoteUrl = "uploads/incidents/i1/p1.jpg").imageModel()

        assertThat(model).isEqualTo("${BuildConfig.DEFAULT_BASE_URL}uploads/incidents/i1/p1.jpg")
    }

    @Test
    fun `a photo with neither has nothing to load`() {
        val model = photo(remoteUrl = null).imageModel()

        assertThat(model).isNull()
    }
}
