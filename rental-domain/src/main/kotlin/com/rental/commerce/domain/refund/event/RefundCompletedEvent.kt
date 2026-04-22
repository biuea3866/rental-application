package com.rental.commerce.domain.refund.event

import com.rental.commerce.domain.refund.RefundStatus
import java.math.BigDecimal

/**
 * 환불 처리 완료 이벤트 (ADR-009 §4).
 *
 * SettlementAdjustmentListener(BE-406) 가 AFTER_COMMIT + REQUIRES_NEW 로 구독해
 * net_amount 보정을 수행. status=FAILED 인 경우에도 이벤트는 발행되며, Listener 가 스킵 결정.
 */
data class RefundCompletedEvent(
    val refundId: Long,
    val paymentId: Long,
    val rentalId: Long,
    val disputeId: Long?,
    val amount: BigDecimal,
    val status: RefundStatus,
)
