package com.rental.commerce.infrastructure.rental

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("prod", "staging")
@EnableConfigurationProperties(TossPaymentProperties::class)
class TossPaymentConfig
