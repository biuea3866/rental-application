package com.rental.commerce.application.notification

import com.rental.commerce.domain.notification.Notification
import com.rental.commerce.domain.notification.NotificationType
import java.time.ZonedDateTime

data class NotificationResponse(
    val notificationId: Long,
    val title: String,
    val message: String,
    val notificationType: NotificationType,
    val referenceId: Long?,
    val referenceType: String?,
    val isRead: Boolean,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse {
            return NotificationResponse(
                notificationId = notification.notificationId,
                title = notification.title,
                message = notification.message,
                notificationType = notification.notificationType,
                referenceId = notification.referenceId,
                referenceType = notification.referenceType,
                isRead = notification.isRead,
                createdAt = notification.createdAt,
            )
        }
    }
}
