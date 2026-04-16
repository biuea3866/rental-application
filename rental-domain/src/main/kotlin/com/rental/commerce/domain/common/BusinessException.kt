package com.rental.commerce.domain.common

open class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
) : RuntimeException(message)

class ResourceNotFoundException(
    errorCode: ErrorCode = ErrorCode.RESOURCE_NOT_FOUND,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)

class DuplicateResourceException(
    errorCode: ErrorCode = ErrorCode.CONFLICT,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)

class InvalidStateTransitionException(
    message: String = ErrorCode.INVALID_STATE_TRANSITION.message,
) : BusinessException(ErrorCode.INVALID_STATE_TRANSITION, message)

class UnauthorizedException(
    errorCode: ErrorCode = ErrorCode.UNAUTHORIZED,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)

class ExpiredTokenException(
    message: String = ErrorCode.EXPIRED_TOKEN.message,
) : BusinessException(ErrorCode.EXPIRED_TOKEN, message)

class InvalidTokenException(
    message: String = ErrorCode.INVALID_TOKEN.message,
) : BusinessException(ErrorCode.INVALID_TOKEN, message)

class TokenFamilyCompromisedException(
    message: String = ErrorCode.TOKEN_FAMILY_COMPROMISED.message,
) : BusinessException(ErrorCode.TOKEN_FAMILY_COMPROMISED, message)

class RentalNotFoundException(
    message: String = ErrorCode.RENTAL_NOT_FOUND.message,
) : BusinessException(ErrorCode.RENTAL_NOT_FOUND, message)

class RentalPeriodConflictException(
    message: String = ErrorCode.RENTAL_PERIOD_CONFLICT.message,
) : BusinessException(ErrorCode.RENTAL_PERIOD_CONFLICT, message)

class PaymentFailedException(
    message: String = ErrorCode.PAYMENT_FAILED.message,
) : BusinessException(ErrorCode.PAYMENT_FAILED, message)

class AlreadyPaidException(
    message: String = ErrorCode.ALREADY_PAID.message,
) : BusinessException(ErrorCode.ALREADY_PAID, message)
