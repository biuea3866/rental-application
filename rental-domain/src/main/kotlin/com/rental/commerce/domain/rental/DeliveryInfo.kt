package com.rental.commerce.domain.rental

data class DeliveryInfo(
    val recipientName: String,
    val recipientPhone: String,
    val addressLine1: String,
    val addressLine2: String?,
    val zipCode: String,
)
