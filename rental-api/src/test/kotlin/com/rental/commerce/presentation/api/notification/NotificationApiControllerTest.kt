package com.rental.commerce.presentation.api.notification

import com.rental.commerce.application.notification.GetNotificationsUseCase
import com.rental.commerce.application.notification.GetUnreadNotificationCountUseCase
import com.rental.commerce.application.notification.MarkAllNotificationsReadUseCase
import com.rental.commerce.application.notification.MarkNotificationReadUseCase
import com.rental.commerce.application.notification.NotificationResponse
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.notification.NotificationType
import com.rental.commerce.presentation.api.common.AuthenticatedRequestWrapper
import com.rental.commerce.presentation.api.common.GlobalExceptionHandler
import com.rental.commerce.presentation.api.common.MemberIdArgumentResolver
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.ZonedDateTime

class NotificationApiControllerTest : BehaviorSpec({

    val getNotificationsUseCase = mockk<GetNotificationsUseCase>()
    val markNotificationReadUseCase = mockk<MarkNotificationReadUseCase>()
    val markAllNotificationsReadUseCase = mockk<MarkAllNotificationsReadUseCase>()
    val getUnreadNotificationCountUseCase = mockk<GetUnreadNotificationCountUseCase>()

    val controller = NotificationApiController(
        getNotificationsUseCase = getNotificationsUseCase,
        markNotificationReadUseCase = markNotificationReadUseCase,
        markAllNotificationsReadUseCase = markAllNotificationsReadUseCase,
        getUnreadNotificationCountUseCase = getUnreadNotificationCountUseCase,
    )

    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setCustomArgumentResolvers(MemberIdArgumentResolver())
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    beforeEach {
        clearMocks(
            getNotificationsUseCase,
            markNotificationReadUseCase,
            markAllNotificationsReadUseCase,
            getUnreadNotificationCountUseCase,
        )
    }

    Given("GET /api/v1/notifications") {

        When("정상적으로 알림 목록을 조회하면") {
            Then("200 OK와 페이지네이션된 알림 목록이 반환된다") {
                val now = ZonedDateTime.now()
                val notifications = listOf(
                    NotificationResponse(
                        notificationId = 1L,
                        title = "상품 승인",
                        message = "상품이 승인되었습니다",
                        notificationType = NotificationType.PRODUCT_APPROVED,
                        referenceId = 100L,
                        referenceType = "PRODUCT",
                        isRead = false,
                        createdAt = now,
                    ),
                    NotificationResponse(
                        notificationId = 2L,
                        title = "시스템 공지",
                        message = "시스템 점검 예정",
                        notificationType = NotificationType.SYSTEM,
                        referenceId = null,
                        referenceType = null,
                        isRead = true,
                        createdAt = now,
                    ),
                )
                val pageResult = PageResult(
                    content = notifications,
                    totalElements = 2L,
                    totalPages = 1,
                )

                every { getNotificationsUseCase.execute(1L, PageQuery(page = 0, size = 20)) } returns pageResult

                val result = mockMvc.get("/api/v1/notifications") {
                    param("page", "0")
                    param("size", "20")
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.content.length()") { value(2) }
                    jsonPath("$.content[0].notificationId") { value(1) }
                    jsonPath("$.content[0].title") { value("상품 승인") }
                    jsonPath("$.content[0].notificationType") { value("PRODUCT_APPROVED") }
                    jsonPath("$.content[0].isRead") { value(false) }
                    jsonPath("$.content[1].notificationId") { value(2) }
                    jsonPath("$.content[1].isRead") { value(true) }
                    jsonPath("$.totalElements") { value(2) }
                }
            }
        }
    }

    Given("PATCH /api/v1/notifications/{id}/read") {

        When("정상적으로 단건 알림을 읽음 처리하면") {
            Then("200 OK가 반환된다") {
                justRun { markNotificationReadUseCase.execute(notificationId = 1L, userId = 1L) }

                val result = mockMvc.patch("/api/v1/notifications/1/read") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                }

                verify(exactly = 1) { markNotificationReadUseCase.execute(notificationId = 1L, userId = 1L) }
            }
        }

        When("존재하지 않는 알림을 읽음 처리하면") {
            Then("404 NOTIFICATION_NOT_FOUND가 반환된다") {
                every {
                    markNotificationReadUseCase.execute(notificationId = 999L, userId = 1L)
                } throws ResourceNotFoundException(
                    errorCode = ErrorCode.NOTIFICATION_NOT_FOUND,
                )

                val result = mockMvc.patch("/api/v1/notifications/999/read") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("NOTIFICATION_NOT_FOUND") }
                }
            }
        }
    }

    Given("PATCH /api/v1/notifications/read-all") {

        When("전체 알림을 읽음 처리하면") {
            Then("200 OK가 반환된다") {
                justRun { markAllNotificationsReadUseCase.execute(userId = 1L) }

                val result = mockMvc.patch("/api/v1/notifications/read-all") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                }

                verify(exactly = 1) { markAllNotificationsReadUseCase.execute(userId = 1L) }
            }
        }
    }

    Given("GET /api/v1/notifications/unread-count") {

        When("읽지 않은 알림 수를 조회하면") {
            Then("200 OK와 미읽음 수가 반환된다") {
                every { getUnreadNotificationCountUseCase.execute(userId = 1L) } returns 5L

                val result = mockMvc.get("/api/v1/notifications/unread-count") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.count") { value(5) }
                }
            }
        }
    }
})
