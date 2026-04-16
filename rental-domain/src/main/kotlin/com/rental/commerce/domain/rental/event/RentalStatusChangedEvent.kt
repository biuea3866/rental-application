package com.rental.commerce.domain.rental.event

import com.rental.commerce.domain.common.DomainEvent
import com.rental.commerce.domain.rental.RentalStatus
import java.time.ZonedDateTime

data class RentalStatusChangedEvent(
    val rentalId: Long,
    val prevStatus: RentalStatus?,
    val newStatus: RentalStatus,
    val renterId: Long,
    val lenderId: Long,
    val productId: Long,
    override val occurredAt: ZonedDateTime = ZonedDateTime.now(),
) : DomainEvent
