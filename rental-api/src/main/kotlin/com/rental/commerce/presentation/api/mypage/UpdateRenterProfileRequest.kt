package com.rental.commerce.presentation.api.mypage

import com.rental.commerce.application.user.UpdateRenterProfileCommand

data class UpdateRenterProfileRequest(
    val shippingAddress: String?,
) {
    fun toCommand(userId: Long): UpdateRenterProfileCommand = UpdateRenterProfileCommand(
        userId = userId,
        shippingAddress = shippingAddress,
    )
}
