package com.rental.commerce.domain.refund

import com.rental.commerce.domain.refund.port.PaymentRefundGateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
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
 */
@Service
class RefundDomainService(
    private val refundRepository: RefundRepository,
    private val paymentRefundGateway: PaymentRefundGateway,
) {
    private val log = LoggerFactory.getLogger(javaClass)

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
