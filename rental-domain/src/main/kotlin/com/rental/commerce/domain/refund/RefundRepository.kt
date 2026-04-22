package com.rental.commerce.domain.refund

import java.math.BigDecimal

interface RefundRepository {

    fun save(refund: Refund): Refund

    fun findById(id: Long): Refund?

    /**
     * 특정 결제에 대한 누적 환불 성공/대기 금액 총합 (중복 방지용).
     * PENDING + SUCCEEDED 를 합산하여 새 환불 요청이 원 결제 금액을 초과하지 않는지 검증.
     */
    fun sumNonFailedAmountByPaymentId(paymentId: Long): BigDecimal

    fun findByDisputeId(disputeId: Long): List<Refund>
}
