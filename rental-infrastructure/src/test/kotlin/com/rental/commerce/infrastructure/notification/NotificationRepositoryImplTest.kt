package com.rental.commerce.infrastructure.notification

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.notification.Notification
import com.rental.commerce.domain.notification.NotificationRepository
import com.rental.commerce.domain.notification.NotificationType
import com.rental.commerce.infrastructure.InfrastructureTestBase
import com.rental.commerce.infrastructure.common.config.JpaAuditingConfig
import com.rental.commerce.infrastructure.common.config.QuerydslConfig
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Import(
    value = [
        JpaAuditingConfig::class,
        QuerydslConfig::class,
        NotificationRepositoryImpl::class,
    ],
)
@ActiveProfiles("test")
class NotificationRepositoryImplTest(
    private val notificationRepository: NotificationRepository,
) : BehaviorSpec({

    extensions(SpringExtension)

    fun createNotification(
        userId: Long = 1L,
        title: String = "테스트 알림",
        message: String = "테스트 알림 내용입니다",
        notificationType: NotificationType = NotificationType.SYSTEM,
        isRead: Boolean = false,
    ): Notification {
        return Notification(
            userId = userId,
            title = title,
            message = message,
            notificationType = notificationType,
            isRead = isRead,
        )
    }

    Given("NotificationRepository - save & findById") {

        When("알림을 저장하고 ID로 조회하면") {
            val saved = notificationRepository.save(createNotification())

            Then("저장된 알림의 ID가 생성된다") {
                saved.id shouldNotBe 0L
            }

            Then("ID로 조회할 수 있다") {
                val found = notificationRepository.findById(saved.id)
                found shouldNotBe null
                found!!.title shouldBe "테스트 알림"
                found.userId shouldBe 1L
            }
        }

        When("존재하지 않는 ID로 조회하면") {
            Then("null을 반환한다") {
                val found = notificationRepository.findById(999999L)
                found shouldBe null
            }
        }
    }

    Given("NotificationRepository - findByUserId") {

        When("유저 ID로 알림 목록을 페이지 조회하면") {
            val userId = 200L
            notificationRepository.save(createNotification(userId = userId, title = "알림1"))
            notificationRepository.save(createNotification(userId = userId, title = "알림2"))
            notificationRepository.save(createNotification(userId = 300L, title = "다른유저알림"))

            val result = notificationRepository.findByUserId(userId, PageQuery(page = 0, size = 10))

            Then("해당 유저의 알림 목록이 반환된다") {
                result.content.size shouldBe 2
                result.totalElements shouldBe 2L
            }
        }

        When("알림이 없는 유저 ID로 조회하면") {
            val result = notificationRepository.findByUserId(999999L, PageQuery(page = 0, size = 10))

            Then("빈 결과가 반환된다") {
                result.content.size shouldBe 0
                result.totalElements shouldBe 0L
                result.totalPages shouldBe 0
            }
        }
    }

    Given("NotificationRepository - countUnreadByUserId") {

        When("읽지 않은 알림이 있는 유저를 조회하면") {
            val userId = 400L
            notificationRepository.save(createNotification(userId = userId, isRead = false))
            notificationRepository.save(createNotification(userId = userId, isRead = false))
            notificationRepository.save(createNotification(userId = userId, isRead = true))

            val count = notificationRepository.countUnreadByUserId(userId)

            Then("읽지 않은 알림 수가 반환된다") {
                count shouldBe 2L
            }
        }

        When("모든 알림을 읽은 유저를 조회하면") {
            val userId = 401L
            notificationRepository.save(createNotification(userId = userId, isRead = true))

            val count = notificationRepository.countUnreadByUserId(userId)

            Then("0이 반환된다") {
                count shouldBe 0L
            }
        }
    }

    Given("NotificationRepository - markAllAsReadByUserId") {

        When("유저의 모든 알림을 읽음 처리하면") {
            val userId = 500L
            notificationRepository.save(createNotification(userId = userId, isRead = false))
            notificationRepository.save(createNotification(userId = userId, isRead = false))

            notificationRepository.markAllAsReadByUserId(userId)

            val unreadCount = notificationRepository.countUnreadByUserId(userId)

            Then("읽지 않은 알림이 0이 된다") {
                unreadCount shouldBe 0L
            }
        }
    }
}) {
    companion object {

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            InfrastructureTestBase.properties(registry)
        }
    }
}
