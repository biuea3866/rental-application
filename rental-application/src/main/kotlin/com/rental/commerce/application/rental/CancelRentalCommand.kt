package com.rental.commerce.application.rental

data class CancelRentalCommand(
    val rentalId: Long,
    val userId: Long,
    val reason: String,
)
