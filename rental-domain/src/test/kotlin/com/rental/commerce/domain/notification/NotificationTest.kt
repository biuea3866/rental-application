package com.rental.commerce.domain.notification

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class NotificationTest : BehaviorSpec({

    fun createNotification(
        userId: Long = 1L,
        title: String = "테스트 알림",
        message: String = "테스트 알림 내용입니다",
        notificationType: NotificationType = NotificationType.SYSTEM,
        referenceId: Long? = null,
        referenceType: String? = null,
    ): Notification {
        return Notification(
            userId = userId,
            title = title,
            message = message,
            notificationType = notificationType,
            referenceId = referenceId,
            referenceType = referenceType,
        )
    }

    Given("markAsRead - 알림 읽음 처리") {

        When("읽지 않은 알림을 markAsRead 하면") {
            val notification = createNotification()

            Then("isRead가 true로 변경된다") {
                notification.isRead shouldBe false
                notification.markAsRead()
                notification.isRead shouldBe true
            }
        }

        When("이미 읽은 알림을 markAsRead 하면") {
            val notification = createNotification()
            notification.markAsRead()

            Then("isRead는 여전히 true이다") {
                notification.markAsRead()
                notification.isRead shouldBe true
            }
        }
    }

    Given("Notification 생성") {

        When("참조 정보 없이 알림을 생성하면") {
            val notification = createNotification(
                userId = 10L,
                title = "시스템 알림",
                message = "시스템 점검 예정입니다",
                notificationType = NotificationType.SYSTEM,
            )

            Then("기본값이 올바르게 설정된다") {
                notification.userId shouldBe 10L
                notification.title shouldBe "시스템 알림"
                notification.message shouldBe "시스템 점검 예정입니다"
                notification.notificationType shouldBe NotificationType.SYSTEM
                notification.referenceId shouldBe null
                notification.referenceType shouldBe null
                notification.isRead shouldBe false
                notification.deletedAt shouldBe null
            }
        }

        When("참조 정보와 함께 알림을 생성하면") {
            val notification = createNotification(
                userId = 5L,
                title = "상품 승인",
                message = "등록하신 상품이 승인되었습니다",
                notificationType = NotificationType.PRODUCT_APPROVED,
                referenceId = 100L,
                referenceType = "PRODUCT",
            )

            Then("참조 정보가 올바르게 설정된다") {
                notification.referenceId shouldBe 100L
                notification.referenceType shouldBe "PRODUCT"
                notification.notificationType shouldBe NotificationType.PRODUCT_APPROVED
            }
        }
    }

    Given("verifyOwner - 소유자 검증") {

        When("알림 소유자 ID와 동일한 userId로 verifyOwner를 호출하면") {
            val notification = createNotification(userId = 1L)

            Then("예외가 발생하지 않는다") {
                notification.verifyOwner(1L)
            }
        }

        When("알림 소유자 ID와 다른 userId로 verifyOwner를 호출하면") {
            val notification = createNotification(userId = 1L)

            Then("BusinessException(FORBIDDEN)이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    notification.verifyOwner(99L)
                }
                exception.errorCode shouldBe ErrorCode.FORBIDDEN
            }
        }
    }
})
