package com.rental.commerce.infrastructure.rental

import com.rental.commerce.domain.common.DomainEvent
import com.rental.commerce.domain.rental.RentalEventPublisher
import com.rental.commerce.domain.rental.event.RentalStatusChangedEvent
import com.rental.commerce.infrastructure.kafka.RentalTopics
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class KafkaRentalEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, RentalStatusChangedEvent>,
) : RentalEventPublisher {

    private val logger = LoggerFactory.getLogger(KafkaRentalEventPublisher::class.java)

    override fun publish(event: DomainEvent) {
        when (event) {
            is RentalStatusChangedEvent -> publishStatusChanged(event)
            else -> logger.warn("[KafkaRentalEventPublisher] 처리되지 않은 이벤트 타입: {}", event::class.simpleName)
        }
    }

    override fun publishAll(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }

    private fun publishStatusChanged(event: RentalStatusChangedEvent) {
        kafkaTemplate.send(
            RentalTopics.STATUS_CHANGED,
            event.rentalId.toString(),
            event,
        ).whenComplete { result, ex ->
            if (ex != null) {
                logger.error(
                    "[KafkaRentalEventPublisher] 이벤트 발행 실패 rentalId={} topic={}",
                    event.rentalId,
                    RentalTopics.STATUS_CHANGED,
                    ex,
                )
            } else {
                logger.info(
                    "[KafkaRentalEventPublisher] 이벤트 발행 성공 rentalId={} topic={} offset={}",
                    event.rentalId,
                    RentalTopics.STATUS_CHANGED,
                    result.recordMetadata.offset(),
                )
            }
        }
    }
}
