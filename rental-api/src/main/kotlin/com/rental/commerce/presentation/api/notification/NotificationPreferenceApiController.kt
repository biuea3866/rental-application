package com.rental.commerce.presentation.api.notification

import com.rental.commerce.application.notification.GetMyNotificationPreferenceUseCase
import com.rental.commerce.application.notification.NotificationPreferenceResult
import com.rental.commerce.application.notification.UpdateNotificationPreferenceCommand
import com.rental.commerce.application.notification.UpdateNotificationPreferenceUseCase
import com.rental.commerce.presentation.api.common.AuthenticatedMember
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/me/notification-preferences")
class NotificationPreferenceApiController(
    private val getMyNotificationPreferenceUseCase: GetMyNotificationPreferenceUseCase,
    private val updateNotificationPreferenceUseCase: UpdateNotificationPreferenceUseCase,
) {

    @GetMapping
    fun getMine(@AuthenticatedMember userId: Long): ResponseEntity<NotificationPreferenceResult> =
        ResponseEntity.ok(getMyNotificationPreferenceUseCase.execute(userId))

    @PatchMapping
    fun updateMine(
        @AuthenticatedMember userId: Long,
        @RequestBody request: UpdateNotificationPreferenceRequest,
    ): ResponseEntity<NotificationPreferenceResult> {
        val result = updateNotificationPreferenceUseCase.execute(request.toCommand(userId))
        return ResponseEntity.ok(result)
    }
}

data class UpdateNotificationPreferenceRequest(
    val chatEnabled: Boolean? = null,
    val rentalEnabled: Boolean? = null,
    val settlementEnabled: Boolean? = null,
    val marketingEnabled: Boolean? = null,
) {
    fun toCommand(userId: Long) = UpdateNotificationPreferenceCommand(
        userId = userId,
        chatEnabled = chatEnabled,
        rentalEnabled = rentalEnabled,
        settlementEnabled = settlementEnabled,
        marketingEnabled = marketingEnabled,
    )
}
