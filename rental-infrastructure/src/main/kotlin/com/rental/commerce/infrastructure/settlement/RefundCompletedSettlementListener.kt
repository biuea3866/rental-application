package com.rental.commerce.infrastructure.settlement

import com.rental.commerce.domain.refund.RefundRepository
import com.rental.commerce.domain.refund.RefundStatus
import com.rental.commerce.domain.refund.event.RefundCompletedEvent
import com.rental.commerce.domain.settlement.SettlementDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * RefundCompletedSettlementListener (BE-406, ADR-009 §4).
 *
 * RefundCompletedEvent 수신 → Settlement.net_amount 재계산.
 *
 * 트랜잭션 경계 (ADR-008 / BE-405 와 동일 패턴):
 *  - @TransactionalEventListener(AFTER_COMMIT)
 *  - Propagation.REQUIRES_NEW
 *
 * Idempotent: net_amount 를 (amount - commission - sum(SUCCEEDED refunds)) 로 매번 재계산.
 * 동일 이벤트 중복 수신 시 계산 결과가 동일해 안전.
 *
 * FAILED 환불 이벤트는 스킵.
 */
@Component
class RefundCompletedSettlementListener(
    private val settlementDomainService: SettlementDomainService,
    private val refundRepository: RefundRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: RefundCompletedEvent) {
        if (event.status != RefundStatus.SUCCEEDED) {
            log.info(
                "[RefundCompletedSettlementListener] SUCCEEDED 아님 — 스킵. refundId={} status={}",
                event.refundId, event.status,
            )
            return
        }

        val totalRefunded = refundRepository.sumSucceededAmountByRentalId(event.rentalId)
        val settlement = settlementDomainService.applyRefundAdjustment(event.rentalId, totalRefunded)

        if (settlement == null) {
            log.warn(
                "[RefundCompletedSettlementListener] Settlement 미존재 — 스킵 " +
                    "(대여 반납 전 환불 가능성). rentalId={} refundId={}",
                event.rentalId, event.refundId,
            )
            return
        }

        log.info(
            "[RefundCompletedSettlementListener] net_amount 보정 완료. " +
                "rentalId={} settlementId={} newNet={} totalRefunded={}",
            event.rentalId, settlement.id, settlement.netAmount, totalRefunded,
        )
    }
}
