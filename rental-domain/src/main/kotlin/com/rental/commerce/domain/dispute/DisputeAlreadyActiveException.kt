package com.rental.commerce.domain.dispute

/**
 * 동일 rental 에 활성 분쟁이 이미 존재할 때. 409 CONFLICT 로 매핑.
 */
class DisputeAlreadyActiveException(message: String) : RuntimeException(message)
