package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.PaymentStatus
import com.rental.commerce.domain.rental.RentalPayment
import com.rental.commerce.domain.common.RentalStatus

data class ProcessPaymentResult(
    val rentalId: Long,
    val paymentId: Long,
    val rentalStatus: RentalStatus,
    val paymentStatus: PaymentStatus,
    val externalPaymentId: String?,
) {
    companion object {
        fun of(rentalId: Long, rentalStatus: RentalStatus, payment: RentalPayment): ProcessPaymentResult =
            ProcessPaymentResult(
                rentalId = rentalId,
                paymentId = payment.id,
                rentalStatus = rentalStatus,
                paymentStatus = payment.status,
                externalPaymentId = payment.externalPaymentId,
            )
    }
}
