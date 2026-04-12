package com.rental.commerce.application.product

data class SubmitProductCommand(
    val userId: Long,
    val productId: Long,
)
