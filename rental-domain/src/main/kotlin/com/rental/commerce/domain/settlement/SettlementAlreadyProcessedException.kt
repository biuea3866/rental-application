package com.rental.commerce.domain.settlement

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode

class SettlementAlreadyProcessedException(
    message: String = ErrorCode.SETTLEMENT_ALREADY_PROCESSED.message,
) : BusinessException(ErrorCode.SETTLEMENT_ALREADY_PROCESSED, message)

class SettlementAlreadyExistsException(
    message: String = ErrorCode.SETTLEMENT_ALREADY_EXISTS.message,
) : BusinessException(ErrorCode.SETTLEMENT_ALREADY_EXISTS, message)
