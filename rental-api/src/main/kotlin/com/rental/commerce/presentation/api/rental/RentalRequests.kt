package com.rental.commerce.presentation.api.rental

import com.rental.commerce.application.rental.ApproveRentalCommand
import com.rental.commerce.application.rental.CancelRentalCommand
import com.rental.commerce.application.rental.DeliveryInfoCommand
import com.rental.commerce.application.rental.GetMyRentalsCommand
import com.rental.commerce.application.rental.ProcessPaymentCommand
import com.rental.commerce.application.rental.RejectRentalCommand
import com.rental.commerce.application.rental.RentalRole
import com.rental.commerce.application.rental.RequestRentalCommand
import com.rental.commerce.application.rental.ReturnRentalCommand
import com.rental.commerce.application.rental.StartRentalCommand
import com.rental.commerce.domain.rental.PaymentMethod
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.ZonedDateTime

data class RequestRentalRequest(
    @field:NotNull val productId: Long?,
    @field:NotNull val lenderId: Long?,
    @field:NotNull val startDate: ZonedDateTime?,
    @field:NotNull val endDate: ZonedDateTime?,
    @field:NotNull @field:Positive val totalAmount: Long?,
    val depositAmount: Long = 0L,
    @field:Valid @field:NotNull val deliveryInfo: DeliveryInfoRequest?,
) {
    fun toCommand(renterId: Long): RequestRentalCommand = RequestRentalCommand(
        renterId = renterId,
        productId = productId!!,
        lenderId = lenderId!!,
        startDate = startDate!!,
        endDate = endDate!!,
        totalAmount = totalAmount!!,
        depositAmount = depositAmount,
        deliveryInfo = deliveryInfo!!.toCommand(),
    )
}

data class DeliveryInfoRequest(
    @field:NotBlank val recipientName: String?,
    @field:NotBlank val recipientPhone: String?,
    @field:NotBlank val addressLine1: String?,
    val addressLine2: String?,
    @field:NotBlank val zipCode: String?,
) {
    fun toCommand(): DeliveryInfoCommand = DeliveryInfoCommand(
        recipientName = recipientName!!,
        recipientPhone = recipientPhone!!,
        addressLine1 = addressLine1!!,
        addressLine2 = addressLine2,
        zipCode = zipCode!!,
    )
}

data class RejectRentalRequest(
    @field:NotBlank val reason: String?,
)

data class ProcessPaymentRequest(
    @field:NotBlank val paymentKey: String?,
    @field:NotBlank val orderId: String?,
    @field:NotNull @field:Positive val amount: Long?,
    @field:NotNull val paymentMethod: PaymentMethod?,
) {
    fun toCommand(rentalId: Long, renterId: Long): ProcessPaymentCommand = ProcessPaymentCommand(
        rentalId = rentalId,
        renterId = renterId,
        paymentKey = paymentKey!!,
        orderId = orderId!!,
        amount = amount!!,
        paymentMethod = paymentMethod!!,
    )
}

data class CancelRentalRequest(
    @field:NotBlank val reason: String?,
)
