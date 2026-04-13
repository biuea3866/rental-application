package com.rental.commerce.application.user

data class UpdateRenterProfileCommand(
    val userId: Long,
    val shippingAddress: String?,
)
