package com.jonesl7l.carinfg.core.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.jonesl7l.carinfg.core.database.dao.IncidentDao
import com.jonesl7l.carinfg.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class PhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val incidentDao: IncidentDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val root: File get() = File(context.filesDir, INCIDENTS_DIR)

    fun photoFile(
        incidentId: String,
        photoId: String,
    ): File = File(
        root,
        "$incidentId/$PHOTOS_DIR/$photoId.jpg",
    ).also { it.parentFile?.mkdirs() }

    fun uriFor(file: File): Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.$PROVIDER_SUFFIX",
        file,
    )

    suspend fun save(
        source: Uri,
        target: File,
    ): Boolean = withContext(ioDispatcher) {
        try {
            val bitmap = decode(source = source) ?: return@withContext false
            val temp = File(target.parentFile, "${target.name}.$TEMP_EXTENSION")
            temp.outputStream().use {
                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    JPEG_QUALITY,
                    it,
                )
            }
            bitmap.recycle()
            temp.renameTo(target)
        } catch (error: IOException) {
            Timber.w(error, "save: could not save photo %s", target.name)
            false
        }
    }

    suspend fun sweep() {
        withContext(ioDispatcher) {
            val incidentIds = incidentDao.getIncidentIds().toSet()
            val photoPaths = incidentDao.getLocalPhotoPaths().toSet()
            root.listFiles()?.forEach { folder ->
                if (folder.name !in incidentIds) {
                    folder.deleteRecursively()
                } else {
                    File(folder, PHOTOS_DIR).listFiles()?.filter { it.absolutePath !in photoPaths }?.forEach { it.delete() }
                }
            }
        }
    }

    private fun decode(source: Uri): Bitmap? {
        val resolver = context.contentResolver
        val orientation =
            resolver.openInputStream(source)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(
                it,
                null,
                bounds,
            )
        }
        val options =
            BitmapFactory.Options().apply {
                inSampleSize =
                    PhotoScaleHelper.sampleSize(
                        width = bounds.outWidth,
                        height = bounds.outHeight,
                        maxEdge = MAX_EDGE,
                    )
            }
        val decoded =
            resolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(
                    it,
                    null,
                    options,
                )
            } ?: return null
        return transform(
            bitmap = decoded,
            orientation = orientation,
        )
    }

    private fun transform(
        bitmap: Bitmap,
        orientation: Int,
    ): Bitmap {
        val scale =
            PhotoScaleHelper.scale(
                width = bitmap.width,
                height = bitmap.height,
                maxEdge = MAX_EDGE,
            )
        val degrees =
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> QUARTER_TURN
                ExifInterface.ORIENTATION_ROTATE_180 -> HALF_TURN
                ExifInterface.ORIENTATION_ROTATE_270 -> THREE_QUARTER_TURN
                else -> 0f
            }
        if (scale == 1f && degrees == 0f) return bitmap
        val matrix =
            Matrix().apply {
                postScale(
                    scale,
                    scale,
                )
                postRotate(degrees)
            }
        val transformed =
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true,
            )
        if (transformed != bitmap) bitmap.recycle()
        return transformed
    }

    private companion object {
        const val INCIDENTS_DIR = "incidents"
        const val PHOTOS_DIR = "photos"
        const val PROVIDER_SUFFIX = "fileprovider"
        const val TEMP_EXTENSION = "tmp"
        const val MAX_EDGE = 2048
        const val JPEG_QUALITY = 80
        const val QUARTER_TURN = 90f
        const val HALF_TURN = 180f
        const val THREE_QUARTER_TURN = 270f
    }
}
