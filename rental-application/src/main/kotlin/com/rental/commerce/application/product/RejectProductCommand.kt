package com.rental.commerce.application.product

data class RejectProductCommand(
    val productId: Long,
    val reason: String,
)
