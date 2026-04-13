package com.rental.commerce.application.notification

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.notification.Notification
import com.rental.commerce.domain.notification.NotificationDomainService
import com.rental.commerce.domain.notification.NotificationType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class GetNotificationsUseCaseTest : BehaviorSpec({

    val notificationDomainService = mockk<NotificationDomainService>()
    val useCase = GetNotificationsUseCase(notificationDomainService)

    beforeEach {
        clearMocks(notificationDomainService)
    }

    Given("사용자 알림 목록 조회") {

        When("알림이 존재하는 사용자의 목록을 조회하면") {
            val userId = 1L
            val pageQuery = PageQuery(page = 0, size = 10)
            val notifications = listOf(
                Notification(
                    id = 1L,
                    userId = userId,
                    title = "상품 승인",
                    message = "상품이 승인되었습니다",
                    notificationType = NotificationType.PRODUCT_APPROVED,
                    referenceId = 100L,
                    referenceType = "PRODUCT",
                ),
                Notification(
                    id = 2L,
                    userId = userId,
                    title = "시스템 공지",
                    message = "시스템 점검 예정입니다",
                    notificationType = NotificationType.SYSTEM,
                ),
            )
            val pageResult = PageResult(
                content = notifications,
                totalElements = 2L,
                totalPages = 1,
            )

            every { notificationDomainService.getNotifications(userId, pageQuery) } returns pageResult

            val result = useCase.execute(userId, pageQuery)

            Then("페이지 정보와 함께 알림 목록이 반환된다") {
                result.content.size shouldBe 2
                result.totalElements shouldBe 2L
                result.totalPages shouldBe 1
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
            val pageQuery = PageQuery(page = 0, size = 10)
            val emptyResult = PageResult<Notification>(
                content = emptyList(),
                totalElements = 0L,
                totalPages = 0,
            )

            every { notificationDomainService.getNotifications(userId, pageQuery) } returns emptyResult

            val result = useCase.execute(userId, pageQuery)

            Then("빈 결과가 반환된다") {
                result.content.size shouldBe 0
                result.totalElements shouldBe 0L
            }
        }
    }
})
