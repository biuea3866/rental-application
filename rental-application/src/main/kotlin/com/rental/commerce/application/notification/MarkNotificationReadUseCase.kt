package com.rental.commerce.application.notification

import com.rental.commerce.domain.notification.NotificationDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MarkNotificationReadUseCase(
    private val notificationDomainService: NotificationDomainService,
) {

    @Transactional
    fun execute(notificationId: Long, userId: Long) {
        val notification = notificationDomainService.getNotificationOrThrow(notificationId)
        notification.verifyOwner(userId)
        notification.markAsRead()
        notificationDomainService.save(notification)
    }
}
