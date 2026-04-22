package com.rental.commerce.domain.dispute.event

import com.rental.commerce.domain.dispute.DisputeReason

/**
 * 분쟁 생성 이벤트 — 양 당사자 알림 트리거 (PRD-004 FR-1.5).
 */
data class DisputeCreatedEvent(
    val disputeId: Long,
    val rentalId: Long,
    val openerId: Long,
    val reason: DisputeReason,
)
