package com.rental.commerce.infrastructure.rental

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "toss.payments")
data class TossPaymentProperties(
    val secretKey: String = "test_sk_placeholder",
    val baseUrl: String = "https://api.tosspayments.com",
    val confirmPath: String = "/v1/payments/confirm",
    val cancelPath: String = "/v1/payments/{paymentKey}/cancel",
)
