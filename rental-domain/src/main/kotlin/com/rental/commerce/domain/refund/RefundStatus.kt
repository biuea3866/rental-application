package com.rental.commerce.domain.refund

/**
 * 환불 상태.
 *
 * PENDING    → PG 호출 전/중
 * SUCCEEDED  → PG 승인 완료
 * FAILED     → PG 실패 (failureReason 기록, 관리자 알림)
 */
enum class RefundStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    ;

    fun validateCanMarkSucceeded() {
        check(this == PENDING) { "환불 완료는 PENDING 상태에서만 가능합니다. current=$this" }
    }

    fun validateCanMarkFailed() {
        check(this == PENDING) { "환불 실패 마킹은 PENDING 상태에서만 가능합니다. current=$this" }
    }
}
