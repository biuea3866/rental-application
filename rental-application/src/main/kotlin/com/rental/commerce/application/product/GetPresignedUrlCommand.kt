package com.rental.commerce.application.product

data class GetPresignedUrlCommand(
    val bucket: String,
    val fileName: String,
    val contentType: String,
)
