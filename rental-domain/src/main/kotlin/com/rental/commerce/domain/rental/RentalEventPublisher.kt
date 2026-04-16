package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.DomainEvent

interface RentalEventPublisher {
    fun publish(event: DomainEvent)
    fun publishAll(events: List<DomainEvent>)
}
