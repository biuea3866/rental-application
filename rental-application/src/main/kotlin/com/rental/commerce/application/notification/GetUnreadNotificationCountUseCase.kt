package com.rental.commerce.application.notification

import com.rental.commerce.domain.notification.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetUnreadNotificationCountUseCase(
    private val notificationRepository: NotificationRepository,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long): Long {
        return notificationRepository.countUnreadByUserId(userId)
    }
}
