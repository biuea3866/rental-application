package com.rental.commerce.domain.product.event

import com.rental.commerce.domain.common.DomainEvent
import java.time.ZonedDateTime

data class ProductApprovedEvent(
    val productId: Long,
    val userId: Long,
    override val occurredAt: ZonedDateTime = ZonedDateTime.now(),
) : DomainEvent
