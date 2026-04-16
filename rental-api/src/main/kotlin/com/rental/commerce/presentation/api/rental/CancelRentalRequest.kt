package com.rental.commerce.presentation.api.rental

import com.rental.commerce.application.rental.CancelRentalCommand
import jakarta.validation.constraints.NotBlank

data class CancelRentalRequest(

    @field:NotBlank(message = "reason은 필수입니다.")
    val reason: String,
) {
    fun toCommand(userId: Long, rentalId: Long): CancelRentalCommand = CancelRentalCommand(
        rentalId = rentalId,
        userId = userId,
        reason = reason,
    )
}
