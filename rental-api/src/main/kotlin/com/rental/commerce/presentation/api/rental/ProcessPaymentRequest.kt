package com.rental.commerce.presentation.api.rental

import com.rental.commerce.application.rental.ProcessPaymentCommand
import com.rental.commerce.domain.rental.PaymentMethod
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class ProcessPaymentRequest(

    @field:NotBlank(message = "paymentKey는 필수입니다.")
    val paymentKey: String,

    @field:NotBlank(message = "orderId는 필수입니다.")
    val orderId: String,

    @field:Positive(message = "amount는 양수여야 합니다.")
    val amount: Long,

    @field:NotNull(message = "paymentMethod는 필수입니다.")
    val paymentMethod: PaymentMethod,
) {
    fun toCommand(userId: Long, rentalId: Long): ProcessPaymentCommand = ProcessPaymentCommand(
        rentalId = rentalId,
        renterId = userId,
        paymentKey = paymentKey,
        orderId = orderId,
        amount = amount,
        paymentMethod = paymentMethod,
    )
}
