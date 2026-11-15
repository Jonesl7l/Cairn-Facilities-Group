package com.jonesl7l.carinfg.core.network.http

object HttpStatus {
    const val OK = 200
    const val CREATED = 201
    const val BAD_REQUEST = 400
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val CONFLICT = 409
    const val GONE = 410
    const val PRECONDITION_FAILED = 412
    const val PAYLOAD_TOO_LARGE = 413
    const val UNSUPPORTED_MEDIA_TYPE = 415
    const val UNPROCESSABLE_CONTENT = 422
    const val PRECONDITION_REQUIRED = 428
    const val INTERNAL_SERVER_ERROR = 500
    const val SERVICE_UNAVAILABLE = 503
    val serverErrors = 500..599
}
