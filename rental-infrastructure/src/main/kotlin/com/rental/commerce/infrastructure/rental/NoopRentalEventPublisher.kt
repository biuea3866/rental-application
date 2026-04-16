package com.rental.commerce.infrastructure.rental

import com.rental.commerce.domain.common.DomainEvent
import com.rental.commerce.domain.rental.RentalEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NoopRentalEventPublisher : RentalEventPublisher {

    private val logger = LoggerFactory.getLogger(NoopRentalEventPublisher::class.java)

    override fun publish(event: DomainEvent) {
        logger.debug("[NoopRentalEventPublisher] 이벤트 발행 (RC-BE-207 Kafka 구현 예정): {}", event)
    }

    override fun publishAll(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }
}
