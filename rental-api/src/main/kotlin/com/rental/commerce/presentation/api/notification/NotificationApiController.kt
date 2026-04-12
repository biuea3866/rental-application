package com.rental.commerce.presentation.api.notification

import com.rental.commerce.application.notification.GetNotificationsUseCase
import com.rental.commerce.application.notification.GetUnreadNotificationCountUseCase
import com.rental.commerce.application.notification.MarkAllNotificationsReadUseCase
import com.rental.commerce.application.notification.MarkNotificationReadUseCase
import com.rental.commerce.application.notification.NotificationResponse
import com.rental.commerce.application.notification.UnreadCountResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
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
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<NotificationResponse>> {
        val userId = extractUserId()
        val pageable = PageRequest.of(page, size)
        val result = getNotificationsUseCase.execute(userId, pageable)
        return ResponseEntity.ok(result)
    }

    @PatchMapping("/{id}/read")
    fun markAsRead(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        val userId = extractUserId()
        markNotificationReadUseCase.execute(notificationId = id, userId = userId)
        return ResponseEntity.ok().build()
    }

    @PatchMapping("/read-all")
    fun markAllAsRead(): ResponseEntity<Void> {
        val userId = extractUserId()
        markAllNotificationsReadUseCase.execute(userId = userId)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(): ResponseEntity<UnreadCountResponse> {
        val userId = extractUserId()
        val count = getUnreadNotificationCountUseCase.execute(userId = userId)
        return ResponseEntity.ok(UnreadCountResponse(count = count))
    }

    private fun extractUserId(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
        return requireNotNull(authentication?.principal as? Long) {
            "인증 정보에서 사용자 ID를 추출할 수 없습니다"
        }
    }
}
