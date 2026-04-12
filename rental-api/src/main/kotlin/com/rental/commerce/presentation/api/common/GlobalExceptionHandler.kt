package com.rental.commerce.presentation.api.common

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(exception: BusinessException): ResponseEntity<ErrorResponse> {
        logger.warn("BusinessException: [${exception.errorCode.code}] ${exception.message}")
        val response = ErrorResponse.of(exception.errorCode, exception.message)
        return ResponseEntity.status(exception.errorCode.httpStatus).body(response)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(exception: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = exception.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        logger.warn("ValidationException: $message")
        val response = ErrorResponse.of(ErrorCode.INVALID_INPUT, message)
        return ResponseEntity.badRequest().body(response)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(exception: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        logger.warn("HttpMessageNotReadableException: ${exception.message}")
        val response = ErrorResponse.of(ErrorCode.INVALID_INPUT, "요청 본문을 읽을 수 없습니다. 입력값을 확인해주세요.")
        return ResponseEntity.badRequest().body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<ErrorResponse> {
        logger.error("UnhandledException: ${exception.message}", exception)
        val response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR)
        return ResponseEntity.internalServerError().body(response)
    }
}
