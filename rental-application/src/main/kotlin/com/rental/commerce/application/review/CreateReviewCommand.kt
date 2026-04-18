package com.rental.commerce.application.review

data class CreateReviewCommand(
    val renterId: Long,
    val rentalId: Long,
    val productId: Long,
    val rating: Int,
    val content: String,
)
