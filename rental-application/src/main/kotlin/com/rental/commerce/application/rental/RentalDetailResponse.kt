package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.PaymentMethod
import com.rental.commerce.domain.rental.PaymentStatus
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalPayment
import com.rental.commerce.domain.rental.RentalStatus
import java.time.ZonedDateTime

data class RentalDetailResponse(
    val rentalId: Long?,
    val renterId: Long,
    val lenderId: Long,
    val productId: Long,
    val status: RentalStatus,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val totalAmount: Long,
    val depositAmount: Long,
    val orderId: String,
    val cancelReason: String?,
    val requestedAt: ZonedDateTime,
    val approvedAt: ZonedDateTime?,
    val paidAt: ZonedDateTime?,
    val startedAt: ZonedDateTime?,
    val returnedAt: ZonedDateTime?,
    val cancelledAt: ZonedDateTime?,
    val deliveryInfo: DeliveryInfoResponse,
    val payment: PaymentDetailResponse?,
) {
    companion object {
        fun from(rental: Rental, payment: RentalPayment?): RentalDetailResponse = RentalDetailResponse(
            rentalId = rental.rentalId,
            renterId = rental.renterId,
            lenderId = rental.lenderId,
            productId = rental.productId,
            status = rental.status,
            startDate = rental.startDate,
            endDate = rental.endDate,
            totalAmount = rental.totalAmount,
            depositAmount = rental.depositAmount,
            orderId = rental.orderId,
            cancelReason = rental.cancelReason,
            requestedAt = rental.requestedAt,
            approvedAt = rental.approvedAt,
            paidAt = rental.paidAt,
            startedAt = rental.startedAt,
            returnedAt = rental.returnedAt,
            cancelledAt = rental.cancelledAt,
            deliveryInfo = DeliveryInfoResponse.from(rental.deliveryInfo),
            payment = payment?.let { PaymentDetailResponse.from(it) },
        )
    }
}

data class PaymentDetailResponse(
    val paymentId: Long?,
    val amount: Long,
    val paymentMethod: PaymentMethod,
    val status: PaymentStatus,
    val externalPaymentId: String?,
    val orderId: String,
    val paidAt: ZonedDateTime?,
    val refundedAt: ZonedDateTime?,
) {
    companion object {
        fun from(payment: RentalPayment): PaymentDetailResponse = PaymentDetailResponse(
            paymentId = payment.paymentId,
            amount = payment.amount,
            paymentMethod = payment.paymentMethod,
            status = payment.status,
            externalPaymentId = payment.externalPaymentId,
            orderId = payment.orderId,
            paidAt = payment.paidAt,
            refundedAt = payment.refundedAt,
        )
    }
}
