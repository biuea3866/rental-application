package com.rental.commerce.infrastructure.notification

import com.rental.commerce.domain.rental.event.RentalStatusChangedEvent
import com.rental.commerce.infrastructure.kafka.RentalTopics
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * RentalStatusChangedKafkaConsumer
 *
 * Kafka 토픽 `event.rental.status-changed` 이벤트를 소비한다.
 * 하네스 규칙:
 * - 원시 문자열 ConsumerRecord 사용 금지 → DTO 직접 매핑
 * - ObjectMapper 수동 파싱 금지 → JsonDeserializer + trusted.packages 설정
 * - Repository 직접 호출 금지 → RentalStatusChangedEventWorker(Facade) 경유
 */
@Component
class RentalStatusChangedKafkaConsumer(
    private val rentalStatusChangedEventWorker: RentalStatusChangedEventWorker,
) {

    private val logger = LoggerFactory.getLogger(RentalStatusChangedKafkaConsumer::class.java)

    @KafkaListener(
        topics = [RentalTopics.STATUS_CHANGED],
        groupId = "\${spring.kafka.consumer.rental-status.group-id:rental-status-notification}",
        containerFactory = "rentalStatusChangedKafkaListenerContainerFactory",
    )
    fun consume(event: RentalStatusChangedEvent) {
        logger.info(
            "[RentalStatusChangedKafkaConsumer] 이벤트 수신 rentalId={} {} → {}",
            event.rentalId,
            event.fromStatus,
            event.toStatus,
        )
        rentalStatusChangedEventWorker.handle(event)
    }
}
