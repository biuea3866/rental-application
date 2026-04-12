package com.rental.commerce.domain.notification

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult

interface NotificationRepository {

    fun save(notification: Notification): Notification

    fun findByUserId(userId: Long, pageQuery: PageQuery): PageResult<Notification>

    fun findById(notificationId: Long): Notification?

    fun countUnreadByUserId(userId: Long): Long

    fun markAllAsReadByUserId(userId: Long)
}
