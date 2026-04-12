package com.rental.commerce.application.notification

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.notification.Notification
import com.rental.commerce.domain.notification.NotificationRepository
import com.rental.commerce.domain.notification.NotificationType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class MarkNotificationReadUseCaseTest : BehaviorSpec({

    val notificationRepository = mockk<NotificationRepository>()
    val useCase = MarkNotificationReadUseCase(notificationRepository)

    beforeEach {
        clearMocks(notificationRepository)
    }

    Given("단건 알림 읽음 처리") {

        When("존재하는 알림을 읽음 처리하면") {

            Then("알림이 읽음 상태로 변경된다") {
                val notification = Notification(
                    notificationId = 1L,
                    userId = 1L,
                    title = "테스트 알림",
                    message = "테스트 메시지",
                    notificationType = NotificationType.SYSTEM,
                )

                every { notificationRepository.findById(1L) } returns notification
                every { notificationRepository.save(any()) } returns notification

                useCase.execute(notificationId = 1L, userId = 1L)

                notification.isRead shouldBe true
            }
        }

        When("존재하지 않는 알림을 읽음 처리하면") {

            Then("ResourceNotFoundException이 발생한다") {
                every { notificationRepository.findById(999L) } returns null

                val exception = shouldThrow<ResourceNotFoundException> {
                    useCase.execute(notificationId = 999L, userId = 1L)
                }
                exception.errorCode shouldBe ErrorCode.NOTIFICATION_NOT_FOUND
            }
        }
    }
})
