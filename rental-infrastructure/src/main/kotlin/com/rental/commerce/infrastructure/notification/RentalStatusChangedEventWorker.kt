package com.rental.commerce.infrastructure.notification

import com.rental.commerce.domain.notification.Notification
import com.rental.commerce.domain.notification.NotificationDomainService
import com.rental.commerce.domain.notification.NotificationType
import com.rental.commerce.domain.rental.RentalStatus
import com.rental.commerce.domain.rental.event.RentalStatusChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * RentalStatusChangedEventWorker
 *
 * RentalStatusChangedEvent 수신 → Notification 생성 + 저장.
 * Observer 패턴 구현체 — 이벤트 타입별로 알림 대상과 메시지를 결정한다.
 *
 * 레이어 규칙:
 * - Repository 직접 호출 금지 → NotificationDomainService 경유
 * - ObjectMapper 수동 파싱 금지 (Kafka Consumer에서 DTO 직접 수신)
 */
@Component
class RentalStatusChangedEventWorker(
    private val notificationDomainService: NotificationDomainService,
) {

    private val logger = LoggerFactory.getLogger(RentalStatusChangedEventWorker::class.java)

    fun handle(event: RentalStatusChangedEvent) {
        val notification = buildNotification(event) ?: return
        notificationDomainService.save(notification)
        logger.info(
            "[RentalStatusChangedEventWorker] 알림 저장 완료 rentalId={} toStatus={} userId={}",
            event.rentalId,
            event.toStatus,
            notification.userId,
        )
    }

    private fun buildNotification(event: RentalStatusChangedEvent): Notification? {
        return when (event.toStatus) {
            RentalStatus.APPROVED -> Notification(
                userId = event.renterId,
                title = "대여 요청이 승인되었습니다",
                message = "rentalId=${event.rentalId} 대여가 승인되었습니다.",
                notificationType = NotificationType.RENTAL_CONFIRMED,
                referenceId = event.rentalId,
                referenceType = "RENTAL",
            )
            RentalStatus.RETURNED -> Notification(
                userId = event.lenderId,
                title = "대여 물품이 반납되었습니다",
                message = "rentalId=${event.rentalId} 대여가 완료되었습니다.",
                notificationType = NotificationType.RENTAL_COMPLETED,
                referenceId = event.rentalId,
                referenceType = "RENTAL",
            )
            RentalStatus.CANCELLED -> Notification(
                userId = event.renterId,
                title = "대여가 취소되었습니다",
                message = "rentalId=${event.rentalId} 대여가 취소되었습니다.",
                notificationType = NotificationType.RENTAL_CANCELLED,
                referenceId = event.rentalId,
                referenceType = "RENTAL",
            )
            else -> {
                logger.debug(
                    "[RentalStatusChangedEventWorker] 알림 불필요 상태: rentalId={} toStatus={}",
                    event.rentalId,
                    event.toStatus,
                )
                null
            }
        }
    }
}
