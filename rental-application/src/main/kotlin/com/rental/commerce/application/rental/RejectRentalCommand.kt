package com.rental.commerce.application.rental

data class RejectRentalCommand(
    val rentalId: Long,
    val userId: Long,
    val reason: String,
)
