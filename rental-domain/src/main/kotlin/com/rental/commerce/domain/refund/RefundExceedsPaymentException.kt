package com.rental.commerce.domain.refund

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode

class RefundExceedsPaymentException(
    message: String = "누적 환불 금액이 원 결제 금액을 초과할 수 없습니다",
) : BusinessException(ErrorCode.INVALID_INPUT, message)

class RefundNotFoundException(
    message: String = "환불 레코드를 찾을 수 없습니다",
) : BusinessException(ErrorCode.RESOURCE_NOT_FOUND, message)
