package com.rental.commerce.presentation.api.rental

import com.rental.commerce.application.rental.RejectRentalCommand
import jakarta.validation.constraints.NotBlank

data class RejectRentalRequest(

    @field:NotBlank(message = "reason은 필수입니다.")
    val reason: String,
) {
    fun toCommand(userId: Long, rentalId: Long): RejectRentalCommand = RejectRentalCommand(
        rentalId = rentalId,
        userId = userId,
        reason = reason,
    )
}
