package com.rental.commerce.infrastructure.payment.gateway

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "payment.toss")
data class TossPaymentProperties(
    val secretKey: String = "",
    val baseUrl: String = "https://api.tosspayments.com",
)
