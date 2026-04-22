package com.rental.commerce.domain.refund.port

import java.math.BigDecimal

/**
 * 환불 외부 PG Port (ADR-009, CLAUDE.md Port-Adapter 규칙).
 *
 * 기존 PaymentGateway.cancelPayment 는 전체 취소. 부분 환불 지원 위해 분리.
 */
interface PaymentRefundGateway {

    /**
     * PG 에 환불을 요청한다.
     *
     * @param paymentKey  원 결제 외부 키 (rental_payment.external_payment_id)
     * @param amount      환불 금액 (전체 혹은 부분)
     * @param reason      환불 사유 (로그/PG 기록용)
     */
    fun requestRefund(paymentKey: String, amount: BigDecimal, reason: String): PaymentRefundResult
}
