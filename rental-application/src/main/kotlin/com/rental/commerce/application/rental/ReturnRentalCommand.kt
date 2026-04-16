package com.rental.commerce.application.rental

data class ReturnRentalCommand(
    val rentalId: Long,
    val userId: Long,
)
