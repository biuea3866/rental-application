package com.rental.commerce.infrastructure.refund

import com.rental.commerce.domain.dispute.DisputeStatus
import com.rental.commerce.domain.dispute.event.DisputeResolvedEvent
import com.rental.commerce.domain.refund.RefundDomainService
import com.rental.commerce.domain.refund.event.RefundCompletedEvent
import com.rental.commerce.domain.rental.RentalPaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.math.BigDecimal

/**
 * DisputeResolvedRefundListener — 분쟁 해결 이벤트 수신 → 환불 처리 (ADR-009 §4, BE-405).
 *
 * 트랜잭션 경계 (ADR-008 과 동일 패턴):
 *  - @TransactionalEventListener(AFTER_COMMIT): 분쟁 해결 트랜잭션 커밋 후 실행
 *  - Propagation.REQUIRES_NEW: 환불 처리는 독립 트랜잭션
 *    → PG 실패가 분쟁 결정을 롤백하지 않음
 *
 * RESOLVED_REJECTED / CANCELLED 이벤트는 환불 대상 아니므로 스킵.
 *
 * CLAUDE.md 예외 규정: 이벤트 리스너는 별도 트랜잭션 경계의 소유자 → Infrastructure 에 @Transactional 허용.
 */
@Component
class DisputeResolvedRefundListener(
    private val refundDomainService: RefundDomainService,
    private val rentalPaymentRepository: RentalPaymentRepository,
    private val eventPublisher: ApplicationEventPublisher,
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
        val amount = event.refundAmount
            ?: run {
                log.warn(
                    "[DisputeResolvedRefundListener] 환불 금액 누락 — 스킵. disputeId={}",
                    event.disputeId,
                )
                return
            }

        val payment = rentalPaymentRepository.findByRentalId(event.rentalId)
            ?: run {
                log.error(
                    "[DisputeResolvedRefundListener] rental_payment 없음 — 환불 불가. rentalId={}",
                    event.rentalId,
                )
                return
            }

        val paymentKey = payment.externalPaymentId
            ?: run {
                log.error(
                    "[DisputeResolvedRefundListener] externalPaymentId 없음 — PG 호출 불가. rentalId={}",
                    event.rentalId,
                )
                return
            }

        val refund = refundDomainService.processRefund(
            paymentId = payment.id,
            paymentKey = paymentKey,
            paymentAmount = BigDecimal.valueOf(payment.amount),
            rentalId = event.rentalId,
            disputeId = event.disputeId,
            amount = amount,
            reason = "DISPUTE_${event.resolution}_${event.disputeId}",
        )

        eventPublisher.publishEvent(
            RefundCompletedEvent(
                refundId = refund.id,
                paymentId = refund.paymentId,
                rentalId = refund.rentalId,
                disputeId = refund.disputeId,
                amount = refund.amount,
                status = refund.status,
            ),
        )
    }

    private fun isRefundTargeted(status: DisputeStatus): Boolean =
        status == DisputeStatus.RESOLVED_REFUND || status == DisputeStatus.RESOLVED_PARTIAL
}
