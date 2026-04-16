package com.rental.commerce.domain.rental

import com.rental.commerce.domain.rental.event.RentalStatusChangedEvent

interface RentalEventPublisher {
    fun publish(event: RentalStatusChangedEvent)
}
