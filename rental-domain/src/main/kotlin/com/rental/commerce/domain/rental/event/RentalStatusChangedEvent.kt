package com.rental.commerce.domain.rental.event

import com.rental.commerce.domain.common.DomainEvent
import com.rental.commerce.domain.rental.RentalStatus
import java.time.ZonedDateTime

data class RentalStatusChangedEvent(
    val rentalId: Long,
    val renterId: Long,
    val lenderId: Long,
    val fromStatus: RentalStatus,
    val toStatus: RentalStatus,
    val rentalAmount: Long = 0L,
    override val occurredAt: ZonedDateTime = ZonedDateTime.now(),
) : DomainEvent
