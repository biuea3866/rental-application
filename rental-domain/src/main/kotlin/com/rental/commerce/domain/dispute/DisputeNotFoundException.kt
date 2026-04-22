package com.rental.commerce.domain.dispute

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode

class DisputeNotFoundException(
    message: String = ErrorCode.DISPUTE_NOT_FOUND.message,
) : BusinessException(ErrorCode.DISPUTE_NOT_FOUND, message)
