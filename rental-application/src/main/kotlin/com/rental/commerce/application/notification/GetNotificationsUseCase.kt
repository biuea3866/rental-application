package com.rental.commerce.application.notification

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.notification.NotificationDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetNotificationsUseCase(
    private val notificationDomainService: NotificationDomainService,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long, pageQuery: PageQuery): PageResult<NotificationResponse> {
        val result = notificationDomainService.getNotifications(userId, pageQuery)
        return PageResult(
            content = result.content.map { NotificationResponse.from(it) },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}
