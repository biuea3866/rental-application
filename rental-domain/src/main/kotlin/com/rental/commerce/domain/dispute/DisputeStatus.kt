package com.rental.commerce.domain.dispute

/**
 * 분쟁 상태 (ADR-009).
 *
 * 활성: OPEN, UNDER_REVIEW — 동일 rental 에 1개만 허용 (DB UNIQUE 보조)
 * 종결: RESOLVED_REFUND, RESOLVED_PARTIAL, RESOLVED_REJECTED, CANCELLED
 */
enum class DisputeStatus {
    OPEN,
    UNDER_REVIEW,
    RESOLVED_REFUND,
    RESOLVED_PARTIAL,
    RESOLVED_REJECTED,
    CANCELLED,
    ;

    fun validateCanStartReview() {
        check(this == OPEN) { "검토 시작은 OPEN 상태에서만 가능합니다. current=$this" }
    }

    fun validateCanResolve() {
        check(this == UNDER_REVIEW) { "해결은 UNDER_REVIEW 상태에서만 가능합니다. current=$this" }
    }

    fun validateCanCancel() {
        check(this == OPEN) { "취소는 OPEN 상태에서만 가능합니다. current=$this" }
    }

    fun isTerminal(): Boolean = this in TERMINAL
    fun isActive(): Boolean = this in ACTIVE

    companion object {
        private val TERMINAL = setOf(RESOLVED_REFUND, RESOLVED_PARTIAL, RESOLVED_REJECTED, CANCELLED)
        private val ACTIVE = setOf(OPEN, UNDER_REVIEW)
    }
}
