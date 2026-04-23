package com.rental.commerce.infrastructure.refund

import com.rental.commerce.domain.dispute.DisputeStatus
import com.rental.commerce.domain.dispute.event.DisputeResolvedEvent
import com.rental.commerce.domain.refund.RefundDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * DisputeResolvedRefundListener — 분쟁 해결 이벤트 수신 → RefundDomainService 로 위임 (ADR-009 §4, BE-405).
 *
 * 책임(최소):
 *  - 환불 대상 여부 판단 (RESOLVED_REFUND / RESOLVED_PARTIAL 외는 스킵)
 *  - amount null 방어
 *  - DomainService 경유 (Repository/Gateway 직접 호출 금지 — 기존 RentalReturnedSettlementListener 패턴 준수)
 *
 * 트랜잭션 경계:
 *  - @TransactionalEventListener(AFTER_COMMIT)
 *  - Propagation.REQUIRES_NEW — PG 장애가 분쟁 결정을 롤백하지 않음
 *
 * CLAUDE.md 예외 규정: 이벤트 리스너는 별도 트랜잭션 경계 소유자 → Infrastructure 에 @Transactional 허용.
 */
@Component
class DisputeResolvedRefundListener(
    private val refundDomainService: RefundDomainService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: DisputeResolvedEvent) {
        if (!isRefundTargeted(event.resolution)) {
            log.info(
                "[DisputeResolvedRefundListener] 환불 대상 아님 — 스킵. disputeId={} resolution={}",
                event.disputeId, event.resolution,
            )
            return
        }
        val amount = event.refundAmount ?: run {
            log.warn(
                "[DisputeResolvedRefundListener] 환불 금액 누락 — 스킵. disputeId={}",
                event.disputeId,
            )
            return
        }

        refundDomainService.processRefundForRental(
            rentalId = event.rentalId,
            disputeId = event.disputeId,
            amount = amount,
            reasonCode = "DISPUTE_${event.resolution}_${event.disputeId}",
        )
    }

    private fun isRefundTargeted(status: DisputeStatus): Boolean =
        status == DisputeStatus.RESOLVED_REFUND || status == DisputeStatus.RESOLVED_PARTIAL
}
