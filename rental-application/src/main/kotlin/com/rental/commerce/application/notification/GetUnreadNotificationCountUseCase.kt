package com.rental.commerce.application.notification

import com.rental.commerce.domain.notification.NotificationDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetUnreadNotificationCountUseCase(
    private val notificationDomainService: NotificationDomainService,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long): Long {
        return notificationDomainService.countUnread(userId)
    }
}
