package com.rental.commerce.application.notification

import com.rental.commerce.domain.notification.Notification
import com.rental.commerce.domain.notification.NotificationRepository
import com.rental.commerce.domain.notification.NotificationType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class GetNotificationsUseCaseTest : BehaviorSpec({

    val notificationRepository = mockk<NotificationRepository>()
    val useCase = GetNotificationsUseCase(notificationRepository)

    beforeEach {
        clearMocks(notificationRepository)
    }

    Given("사용자 알림 목록 조회") {

        When("알림이 존재하는 사용자의 목록을 조회하면") {
            val userId = 1L
            val pageable = PageRequest.of(0, 10)
            val notifications = listOf(
                Notification(
                    notificationId = 1L,
                    userId = userId,
                    title = "상품 승인",
                    message = "상품이 승인되었습니다",
                    notificationType = NotificationType.PRODUCT_APPROVED,
                    referenceId = 100L,
                    referenceType = "PRODUCT",
                ),
                Notification(
                    notificationId = 2L,
                    userId = userId,
                    title = "시스템 공지",
                    message = "시스템 점검 예정입니다",
                    notificationType = NotificationType.SYSTEM,
                ),
            )
            val page = PageImpl(notifications, pageable, 2L)

            every { notificationRepository.findByUserId(userId, pageable) } returns page

            val result = useCase.execute(userId, pageable)

            Then("페이지 정보와 함께 알림 목록이 반환된다") {
                result.content.size shouldBe 2
                result.totalElements shouldBe 2L
            }

            Then("알림 정보가 올바르게 매핑된다") {
                val first = result.content[0]
                first.notificationId shouldBe 1L
                first.title shouldBe "상품 승인"
                first.message shouldBe "상품이 승인되었습니다"
                first.notificationType shouldBe NotificationType.PRODUCT_APPROVED
                first.referenceId shouldBe 100L
                first.referenceType shouldBe "PRODUCT"
                first.isRead shouldBe false
            }
        }

        When("알림이 없는 사용자의 목록을 조회하면") {
            val userId = 999L
            val pageable = PageRequest.of(0, 10)
            val emptyPage = PageImpl<Notification>(emptyList(), pageable, 0L)

            every { notificationRepository.findByUserId(userId, pageable) } returns emptyPage

            val result = useCase.execute(userId, pageable)

            Then("빈 페이지가 반환된다") {
                result.content.size shouldBe 0
                result.totalElements shouldBe 0L
            }
        }
    }
})
