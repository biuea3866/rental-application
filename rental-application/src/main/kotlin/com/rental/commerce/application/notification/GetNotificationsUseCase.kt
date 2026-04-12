package com.rental.commerce.application.notification

import com.rental.commerce.domain.notification.NotificationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetNotificationsUseCase(
    private val notificationRepository: NotificationRepository,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long, pageable: Pageable): Page<NotificationResponse> {
        val notifications = notificationRepository.findByUserId(userId, pageable)
        return notifications.map { NotificationResponse.from(it) }
    }
}
