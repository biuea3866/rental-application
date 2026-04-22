package com.rental.commerce.domain.dispute

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode

/**
 * 동일 rental 에 활성 분쟁이 이미 존재할 때 (409 CONFLICT).
 */
class DisputeAlreadyActiveException(
    message: String = ErrorCode.DISPUTE_ALREADY_ACTIVE.message,
) : BusinessException(ErrorCode.DISPUTE_ALREADY_ACTIVE, message)
