package com.jonesl7l.carinfg.core.data.photo

object PhotoScaleHelper {
    // the largest power of two that still leaves the long edge at or above maxEdge
    fun sampleSize(
        width: Int,
        height: Int,
        maxEdge: Int,
    ): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }

    fun scale(
        width: Int,
        height: Int,
        maxEdge: Int,
    ): Float = minOf(1f, maxEdge.toFloat() / maxOf(width, height))
}
