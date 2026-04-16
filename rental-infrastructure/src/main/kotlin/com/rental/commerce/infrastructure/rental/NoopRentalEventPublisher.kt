package com.rental.commerce.infrastructure.rental

import com.rental.commerce.domain.rental.RentalEventPublisher
import com.rental.commerce.domain.rental.event.RentalStatusChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NoopRentalEventPublisher : RentalEventPublisher {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(event: RentalStatusChangedEvent) {
        log.info("[NoopRentalEventPublisher] rental={} status={} → {} (Kafka 연동 전 no-op)", event.rentalId, event.prevStatus, event.newStatus)
    }
}
