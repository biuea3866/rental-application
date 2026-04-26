package com.rental.commerce.domain.refund

import com.rental.commerce.domain.refund.event.RefundCompletedEvent
import com.rental.commerce.domain.refund.port.PaymentRefundGateway
import com.rental.commerce.domain.rental.RentalPaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * RefundDomainService — 환불 처리 오케스트레이션 (ADR-009 §4).
 *
 * 처리 흐름:
 *  1. 원 결제 금액 vs 누적 환불 금액 검증 (누적 초과 시 예외)
 *  2. Refund(status=PENDING) 저장
 *  3. PaymentRefundGateway 호출
 *  4. 결과에 따라 markSucceeded / markFailed
 *
 * SELECT ... FOR UPDATE 는 Repository 구현체가 담당 (인프라 레이어).
 *
 * 트랜잭션 정책 (CLAUDE.md — @Transactional 은 UseCase / DomainService 에서만):
 *  - 클래스 기본: @Transactional (쓰기 포함 메서드가 대부분)
 *  - 모든 public 메서드는 쓰기 작업을 포함하므로 readOnly = false 유지
 */
@Service
@Transactional
class RefundDomainService(
    private val refundRepository: RefundRepository,
    private val paymentRefundGateway: PaymentRefundGateway,
    private val rentalPaymentRepository: RentalPaymentRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * rental 기준 환불 처리 + 완료 이벤트 발행 (BE-405 Listener 가 호출).
     *
     * Infrastructure Listener 에 비즈니스 로직이 새지 않도록, 결제 정보 조회 + PG 호출 +
     * RefundCompletedEvent 발행까지 이 메서드에서 일원화한다 (ADR-009 §4, pr-reviewer 지적).
     *
     * 도메인 경계: 분쟁 도메인은 의존하지 않고 primitive(rentalId, disputeId?, amount, reasonCode)
     * 만 받음.
     */
    fun processRefundForRental(
        rentalId: Long,
        disputeId: Long?,
        amount: BigDecimal,
        reasonCode: String,
    ): Refund? {
        val payment = rentalPaymentRepository.findByRentalId(rentalId)
            ?: run {
                log.error(
                    "[RefundDomainService.processRefundForRental] rental_payment 없음. rentalId={}",
                    rentalId,
                )
                return null
            }

        val paymentKey = payment.externalPaymentId
            ?: run {
                log.error(
                    "[RefundDomainService.processRefundForRental] externalPaymentId 없음. rentalId={}",
                    rentalId,
                )
                return null
            }

        val refund = processRefund(
            paymentId = payment.id,
            paymentKey = paymentKey,
            paymentAmount = BigDecimal.valueOf(payment.amount),
            rentalId = rentalId,
            disputeId = disputeId,
            amount = amount,
            reason = reasonCode,
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
        return refund
    }

    fun processRefund(
        paymentId: Long,
        paymentKey: String,
        paymentAmount: BigDecimal,
        rentalId: Long,
        disputeId: Long?,
        amount: BigDecimal,
        reason: String,
    ): Refund {
        val alreadyRefunded = refundRepository.sumNonFailedAmountByPaymentId(paymentId)
        if (alreadyRefunded + amount > paymentAmount) {
            throw RefundExceedsPaymentException(
                "paymentId=$paymentId alreadyRefunded=$alreadyRefunded + requested=$amount > paymentAmount=$paymentAmount",
            )
        }

        val refund = Refund.create(
            paymentId = paymentId,
            rentalId = rentalId,
            disputeId = disputeId,
            amount = amount,
            reason = reason,
        )
        val saved = refundRepository.save(refund)

        val pgResult = runCatching {
            paymentRefundGateway.requestRefund(paymentKey, amount, reason)
        }.getOrElse { ex ->
            log.error("[RefundDomainService] PG 호출 예외 refundId={} error={}", saved.id, ex.message, ex)
            saved.markFailed(ex.message ?: "PG call threw exception")
            return refundRepository.save(saved)
        }

        if (pgResult.success && pgResult.externalRefundId != null) {
            saved.markSucceeded(pgResult.externalRefundId)
        } else {
            saved.markFailed(pgResult.failureReason ?: "Unknown PG failure")
            log.warn("[RefundDomainService] PG 환불 실패 refundId={} reason={}", saved.id, pgResult.failureReason)
        }
        return refundRepository.save(saved)
    }
}
