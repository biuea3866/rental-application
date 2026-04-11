package com.rental.commerce.domain.common

import java.time.ZonedDateTime

interface DomainEvent {
    val occurredAt: ZonedDateTime
        get() = ZonedDateTime.now()
}
