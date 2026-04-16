package com.rental.commerce.presentation.api.rental

import com.rental.commerce.application.rental.RequestRentalCommand
import com.rental.commerce.domain.rental.DeliveryInfo
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.ZonedDateTime

data class CreateRentalRequest(

    @field:NotNull(message = "productId는 필수입니다.")
    val productId: Long,

    @field:NotNull(message = "startDate는 필수입니다.")
    val startDate: ZonedDateTime,

    @field:NotNull(message = "endDate는 필수입니다.")
    val endDate: ZonedDateTime,

    @field:Positive(message = "dailyPrice는 양수여야 합니다.")
    val dailyPrice: Long,

    @field:Valid
    @field:NotNull(message = "deliveryInfo는 필수입니다.")
    val deliveryInfo: DeliveryInfoRequest,
) {
    fun toCommand(userId: Long): RequestRentalCommand = RequestRentalCommand(
        renterId = userId,
        productId = productId,
        startDate = startDate,
        endDate = endDate,
        dailyPrice = dailyPrice,
        deliveryInfo = DeliveryInfo(
            recipientName = deliveryInfo.recipientName,
            recipientPhone = deliveryInfo.recipientPhone,
            addressLine1 = deliveryInfo.addressLine1,
            addressLine2 = deliveryInfo.addressLine2,
            zipCode = deliveryInfo.zipCode,
        ),
    )
}

data class DeliveryInfoRequest(

    @field:NotBlank(message = "recipientName은 필수입니다.")
    val recipientName: String,

    @field:NotBlank(message = "recipientPhone은 필수입니다.")
    val recipientPhone: String,

    @field:NotBlank(message = "addressLine1은 필수입니다.")
    val addressLine1: String,

    val addressLine2: String? = null,

    @field:NotBlank(message = "zipCode는 필수입니다.")
    val zipCode: String,
)
