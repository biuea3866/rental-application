package com.rental.commerce.application.notification

import com.rental.commerce.domain.notification.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MarkAllNotificationsReadUseCase(
    private val notificationRepository: NotificationRepository,
) {

    @Transactional
    fun execute(userId: Long) {
        notificationRepository.markAllAsReadByUserId(userId)
    }
}
