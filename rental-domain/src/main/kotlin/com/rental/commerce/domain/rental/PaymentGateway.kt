package com.rental.commerce.domain.rental

import java.time.ZonedDateTime

interface PaymentGateway {
    fun approve(request: PaymentApproveRequest): PaymentApproveResult
    fun cancel(paymentKey: String, cancelAmount: Long, reason: String): PaymentCancelResult
}

data class PaymentApproveRequest(
    val paymentKey: String,
    val orderId: String,
    val amount: Long,
)

data class PaymentApproveResult(
    val externalPaymentId: String,
    val approvedAt: ZonedDateTime,
    val method: String,
)

data class PaymentCancelResult(
    val paymentKey: String,
    val cancelledAt: ZonedDateTime,
    val cancelAmount: Long,
)
