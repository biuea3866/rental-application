package com.rental.commerce.application.notification

import com.rental.commerce.domain.notification.NotificationDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MarkAllNotificationsReadUseCase(
    private val notificationDomainService: NotificationDomainService,
) {

    @Transactional
    fun execute(userId: Long) {
        notificationDomainService.markAllAsRead(userId)
    }
}
