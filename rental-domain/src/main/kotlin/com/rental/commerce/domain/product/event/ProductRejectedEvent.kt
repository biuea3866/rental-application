package com.rental.commerce.domain.product.event

import com.rental.commerce.domain.common.DomainEvent
import java.time.ZonedDateTime

data class ProductRejectedEvent(
    val productId: Long,
    val userId: Long,
    val reason: String,
    override val occurredAt: ZonedDateTime = ZonedDateTime.now(),
) : DomainEvent
