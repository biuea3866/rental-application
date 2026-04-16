package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.PaymentMethod

data class ProcessPaymentCommand(
    val rentalId: Long,
    val renterId: Long,
    val paymentKey: String,
    val orderId: String,
    val amount: Long,
    val paymentMethod: PaymentMethod,
)
