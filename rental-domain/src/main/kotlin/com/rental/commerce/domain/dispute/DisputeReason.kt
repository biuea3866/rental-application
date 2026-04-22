package com.rental.commerce.domain.dispute

/**
 * 분쟁 사유 (PRD-004 용어 정의).
 */
enum class DisputeReason {
    DAMAGED,
    NOT_RETURNED,
    LATE_RETURN,
    WRONG_ITEM,
    OTHER,
}
