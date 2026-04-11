package com.rental.commerce.presentation.api.common

import com.rental.commerce.domain.common.ErrorCode
import java.time.ZonedDateTime

data class ErrorResponse(
    val code: String,
    val message: String,
    val status: Int,
    val timestamp: ZonedDateTime = ZonedDateTime.now(),
) {
    companion object {
        fun of(errorCode: ErrorCode): ErrorResponse = ErrorResponse(
            code = errorCode.code,
            message = errorCode.message,
            status = errorCode.httpStatus,
        )

        fun of(errorCode: ErrorCode, customMessage: String): ErrorResponse = ErrorResponse(
            code = errorCode.code,
            message = customMessage,
            status = errorCode.httpStatus,
        )
    }
}
