package com.rental.commerce.domain.rental

import java.time.ZonedDateTime

data class PaymentCancelResult(
    val success: Boolean,
    val cancelAmount: Long,
    val canceledAt: ZonedDateTime?,
)
