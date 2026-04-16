package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.PaymentMethod
import java.time.ZonedDateTime

data class DeliveryInfoCommand(
    val recipientName: String,
    val recipientPhone: String,
    val addressLine1: String,
    val addressLine2: String?,
    val zipCode: String,
) {
    fun toDomain(): DeliveryInfo = DeliveryInfo(
        recipientName = recipientName,
        recipientPhone = recipientPhone,
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        zipCode = zipCode,
    )
}

data class RequestRentalCommand(
    val renterId: Long,
    val productId: Long,
    val lenderId: Long,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val totalAmount: Long,
    val depositAmount: Long,
    val deliveryInfo: DeliveryInfoCommand,
)

data class ApproveRentalCommand(
    val rentalId: Long,
    val lenderId: Long,
)

data class RejectRentalCommand(
    val rentalId: Long,
    val lenderId: Long,
    val reason: String,
)

data class ProcessPaymentCommand(
    val rentalId: Long,
    val renterId: Long,
    val paymentKey: String,
    val orderId: String,
    val amount: Long,
    val paymentMethod: PaymentMethod,
)

data class StartRentalCommand(
    val rentalId: Long,
    val lenderId: Long,
)

data class ReturnRentalCommand(
    val rentalId: Long,
    val renterId: Long,
)

data class CancelRentalCommand(
    val rentalId: Long,
    val userId: Long,
    val reason: String,
)

data class GetMyRentalsCommand(
    val userId: Long,
    val role: RentalRole,
)

enum class RentalRole {
    RENTER,
    LENDER,
}
