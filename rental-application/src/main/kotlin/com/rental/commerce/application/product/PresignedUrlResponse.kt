package com.rental.commerce.application.product

data class PresignedUrlResponse(
    val uploadUrl: String,
    val objectKey: String,
)
