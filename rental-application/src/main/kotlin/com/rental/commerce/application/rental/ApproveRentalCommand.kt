package com.rental.commerce.application.rental

data class ApproveRentalCommand(
    val rentalId: Long,
    val userId: Long,
)
