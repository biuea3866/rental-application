package com.rental.commerce.domain.rental

import java.time.ZonedDateTime

/**
 * RentalPayment Entity — Rental과 1:1 결제 레코드.
 * JPA 어노테이션은 rental-wt-201(RC-201) 브랜치에서 추가됩니다.
 */
class RentalPayment(
    val paymentId: Long? = null,
    val rentalId: Long,
    val amount: Long,
    val paymentMethod: PaymentMethod,
    var status: PaymentStatus = PaymentStatus.PENDING,
    var externalPaymentId: String? = null,
    val orderId: String,
    var paidAt: ZonedDateTime? = null,
    var refundedAt: ZonedDateTime? = null,
) {

    fun complete(externalId: String) {
        status = PaymentStatus.COMPLETED
        externalPaymentId = externalId
        paidAt = ZonedDateTime.now()
    }

    fun fail() {
        status = PaymentStatus.FAILED
    }

    fun refund() {
        status = PaymentStatus.REFUNDED
        refundedAt = ZonedDateTime.now()
    }

    companion object {
        fun pending(
            rentalId: Long,
            amount: Long,
            paymentMethod: PaymentMethod,
            orderId: String,
        ): RentalPayment {
            return RentalPayment(
                rentalId = rentalId,
                amount = amount,
                paymentMethod = paymentMethod,
                status = PaymentStatus.PENDING,
                orderId = orderId,
            )
        }
    }
}
