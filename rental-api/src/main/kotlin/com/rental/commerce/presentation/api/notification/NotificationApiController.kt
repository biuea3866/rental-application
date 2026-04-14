package com.rental.commerce.presentation.api.notification

import com.rental.commerce.application.notification.GetNotificationsUseCase
import com.rental.commerce.application.notification.GetUnreadNotificationCountUseCase
import com.rental.commerce.application.notification.MarkAllNotificationsReadUseCase
import com.rental.commerce.application.notification.MarkNotificationReadUseCase
import com.rental.commerce.application.notification.NotificationResponse
import com.rental.commerce.application.notification.UnreadCountResponse
import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.presentation.api.common.AuthenticatedMember
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationApiController(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase,
    private val markAllNotificationsReadUseCase: MarkAllNotificationsReadUseCase,
    private val getUnreadNotificationCountUseCase: GetUnreadNotificationCountUseCase,
) {

    @GetMapping
    fun getNotifications(
        @AuthenticatedMember userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResult<NotificationResponse>> {
        val pageQuery = PageQuery(page = page, size = size)
        val result = getNotificationsUseCase.execute(userId, pageQuery)
        return ResponseEntity.ok(result)
    }

    @PatchMapping("/{id}/read")
    fun markAsRead(
        @AuthenticatedMember userId: Long,
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        markNotificationReadUseCase.execute(notificationId = id, userId = userId)
        return ResponseEntity.ok().build()
    }

    @PatchMapping("/read-all")
    fun markAllAsRead(
        @AuthenticatedMember userId: Long,
    ): ResponseEntity<Void> {
        markAllNotificationsReadUseCase.execute(userId = userId)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(
        @AuthenticatedMember userId: Long,
    ): ResponseEntity<UnreadCountResponse> {
        val count = getUnreadNotificationCountUseCase.execute(userId = userId)
        return ResponseEntity.ok(UnreadCountResponse(count = count))
    }
}
