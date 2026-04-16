package com.rental.commerce.domain.rental

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class DeliveryInfo(

    @Column(name = "recipient_name", length = 50, nullable = false)
    val recipientName: String,

    @Column(name = "recipient_phone", length = 20, nullable = false)
    val recipientPhone: String,

    @Column(name = "address_line1", length = 200, nullable = false)
    val addressLine1: String,

    @Column(name = "address_line2", length = 100)
    val addressLine2: String? = null,

    @Column(name = "zip_code", length = 10, nullable = false)
    val zipCode: String,
)
