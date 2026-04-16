package com.rental.commerce.domain.rental

import java.time.ZonedDateTime

data class PaymentResult(
    val success: Boolean,
    val paymentKey: String,
    val orderId: String,
    val amount: Long,
    val approvedAt: ZonedDateTime?,
    val failureMessage: String?,
)
