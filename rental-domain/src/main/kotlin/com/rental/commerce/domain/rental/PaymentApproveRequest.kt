package com.rental.commerce.domain.rental

data class PaymentApproveRequest(
    val orderId: String,
    val amount: Long,
    val paymentKey: String,
)
