package com.rental.commerce.domain.dispute

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode

/**
 * 분쟁 접근/조작 권한 부재 (403).
 */
class DisputeForbiddenException(
    message: String = ErrorCode.DISPUTE_FORBIDDEN.message,
) : BusinessException(ErrorCode.DISPUTE_FORBIDDEN, message)
