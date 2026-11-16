package com.jonesl7l.carinfg.core.ui.photo

import com.jonesl7l.carinfg.BuildConfig
import com.jonesl7l.carinfg.core.domain.model.photo.Photo
import java.io.File
import okhttp3.HttpUrl.Companion.toHttpUrl

private val baseUrl = BuildConfig.DEFAULT_BASE_URL.toHttpUrl()

fun Photo.imageModel(): Any? = localPath?.let { File(it) } ?: remoteUrl?.let { baseUrl.resolve(link = it)?.toString() }
