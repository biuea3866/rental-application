package com.rental.commerce.application.rental

data class StartRentalCommand(
    val rentalId: Long,
    val userId: Long,
)
