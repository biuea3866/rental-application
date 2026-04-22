package com.rental.commerce.domain.dispute.event

import com.rental.commerce.domain.dispute.DisputeStatus
import java.math.BigDecimal

/**
 * 분쟁 해결 이벤트 (ADR-009 §4).
 *
 * RefundProcessingListener 가 AFTER_COMMIT + REQUIRES_NEW 로 구독하여 환불을 처리한다.
 * resolution 이 RESOLVED_REJECTED / CANCELLED 면 refundAmount 는 null (환불 없음).
 */
data class DisputeResolvedEvent(
    val disputeId: Long,
    val rentalId: Long,
    val resolution: DisputeStatus,
    val refundAmount: BigDecimal?,
)
