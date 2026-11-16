package com.jonesl7l.carinfg.core.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.inMemoryDatabase
import com.jonesl7l.carinfg.core.database.EntityTestHelper.INCIDENT_ID
import com.jonesl7l.carinfg.core.database.EntityTestHelper.incidentEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.photoEntity
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoStorageTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val database: AppDatabase = inMemoryDatabase()
    private val storage =
        PhotoStorage(
            context = context,
            incidentDao = database.incidentDao(),
            ioDispatcher = Dispatchers.IO,
        )

    @After
    fun tearDown() {
        database.close()
        File(context.filesDir, "incidents").deleteRecursively()
    }

    @Test
    fun aLargePhotoIsScaledDownToTheLimit() = runTest {
        val source = jpeg(
            width = 4096,
            height = 3072,
        )
        val target = storage.photoFile(
            incidentId = INCIDENT_ID,
            photoId = "photo-1",
        )

        assertThat(
            storage.save(
                source = Uri.fromFile(source),
                target = target,
            ),
        ).isTrue()

        val bounds = bounds(file = target)
        assertThat(bounds.outWidth).isEqualTo(2048)
        assertThat(bounds.outHeight).isEqualTo(1536)
    }

    @Test
    fun aRotatedPhotoIsTurnedUpright() = runTest {
        val source = jpeg(
            width = 400,
            height = 300,
        )
        ExifInterface(source).apply {
            setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_ROTATE_90.toString(),
            )
            saveAttributes()
        }
        val target = storage.photoFile(
            incidentId = INCIDENT_ID,
            photoId = "photo-1",
        )

        storage.save(
            source = Uri.fromFile(source),
            target = target,
        )

        val bounds = bounds(file = target)
        assertThat(bounds.outWidth).isEqualTo(300)
        assertThat(bounds.outHeight).isEqualTo(400)
    }

    @Test
    fun theSweepRemovesFilesAndFoldersWithNoRow() = runTest {
        val kept = storage.photoFile(
            incidentId = INCIDENT_ID,
            photoId = "photo-0",
        ).apply { writeText(text = "kept") }
        val orphanPhoto = storage.photoFile(
            incidentId = INCIDENT_ID,
            photoId = "photo-9",
        ).apply { writeText(text = "orphan") }
        val orphanFolder = storage.photoFile(
            incidentId = "gone",
            photoId = "photo-0",
        ).apply { writeText(text = "orphan") }
        database.incidentDao().upsertIncident(incident = incidentEntity())
        database.incidentDao().upsertPhotos(photos = listOf(photoEntity(localPath = kept.absolutePath)))

        storage.sweep()

        assertThat(kept.exists()).isTrue()
        assertThat(orphanPhoto.exists()).isFalse()
        assertThat(orphanFolder.parentFile?.parentFile?.exists() ?: false).isFalse()
    }

    private fun jpeg(
        width: Int,
        height: Int,
    ): File {
        val file = File(context.cacheDir, "source.jpg")
        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888,
        )
        file.outputStream().use {
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                JPEG_QUALITY,
                it,
            )
        }
        return file
    }

    private fun bounds(file: File) = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
        BitmapFactory.decodeFile(
            file.absolutePath,
            this,
        )
    }

    private companion object {
        const val JPEG_QUALITY = 90
    }
}
