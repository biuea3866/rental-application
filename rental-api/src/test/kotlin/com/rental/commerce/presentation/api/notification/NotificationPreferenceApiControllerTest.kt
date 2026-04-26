package com.rental.commerce.presentation.api.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.rental.commerce.application.notification.GetMyNotificationPreferenceUseCase
import com.rental.commerce.application.notification.NotificationPreferenceResult
import com.rental.commerce.application.notification.UpdateNotificationPreferenceUseCase
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.presentation.api.common.AuthenticatedRequestWrapper.Companion.HEADER_USER_ID
import com.rental.commerce.presentation.api.common.GlobalExceptionHandler
import com.rental.commerce.presentation.api.common.MemberIdArgumentResolver
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class NotificationPreferenceApiControllerTest : BehaviorSpec({

    val getMyNotificationPreferenceUseCase = mockk<GetMyNotificationPreferenceUseCase>()
    val updateNotificationPreferenceUseCase = mockk<UpdateNotificationPreferenceUseCase>()

    val controller = NotificationPreferenceApiController(
        getMyNotificationPreferenceUseCase = getMyNotificationPreferenceUseCase,
        updateNotificationPreferenceUseCase = updateNotificationPreferenceUseCase,
    )

    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setCustomArgumentResolvers(MemberIdArgumentResolver())
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    val objectMapper = ObjectMapper().apply {
        findAndRegisterModules()
    }

    val samplePreferenceResult = NotificationPreferenceResult(
        userId = 42L,
        chatEnabled = true,
        rentalEnabled = true,
        settlementEnabled = false,
        marketingEnabled = false,
    )

    beforeEach {
        clearMocks(getMyNotificationPreferenceUseCase, updateNotificationPreferenceUseCase)
    }

    Given("GET /api/v1/me/notification-preferences") {

        When("정상적으로 알림 설정을 조회하면") {
            Then("200 OK와 NotificationPreferenceResult가 반환된다") {
                every { getMyNotificationPreferenceUseCase.execute(42L) } returns samplePreferenceResult

                val result = mockMvc.get("/api/v1/me/notification-preferences") {
                    header(HEADER_USER_ID, "42")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.userId") { value(42) }
                    jsonPath("$.chatEnabled") { value(true) }
                    jsonPath("$.rentalEnabled") { value(true) }
                    jsonPath("$.settlementEnabled") { value(false) }
                    jsonPath("$.marketingEnabled") { value(false) }
                }

                verify(exactly = 1) { getMyNotificationPreferenceUseCase.execute(42L) }
            }
        }

        When("알림 설정이 존재하지 않으면") {
            Then("404 NOTIFICATION_PREFERENCE_NOT_FOUND가 반환된다") {
                every {
                    getMyNotificationPreferenceUseCase.execute(999L)
                } throws ResourceNotFoundException(
                    errorCode = ErrorCode.NOTIFICATION_PREFERENCE_NOT_FOUND,
                )

                val result = mockMvc.get("/api/v1/me/notification-preferences") {
                    header(HEADER_USER_ID, "999")
                }

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("NOTIFICATION_PREFERENCE_NOT_FOUND") }
                }
            }
        }

        When("X-Member-Id 헤더가 없으면") {
            Then("401 UNAUTHORIZED가 반환된다") {
                val result = mockMvc.get("/api/v1/me/notification-preferences")

                result.andExpect {
                    status { isUnauthorized() }
                }
            }
        }
    }

    Given("PATCH /api/v1/me/notification-preferences") {

        When("chatEnabled만 업데이트하면") {
            Then("200 OK와 업데이트된 NotificationPreferenceResult가 반환된다") {
                val updatedResult = samplePreferenceResult.copy(chatEnabled = false)
                every { updateNotificationPreferenceUseCase.execute(any()) } returns updatedResult

                val requestBody = mapOf("chatEnabled" to false)

                val result = mockMvc.patch("/api/v1/me/notification-preferences") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requestBody)
                    header(HEADER_USER_ID, "42")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.chatEnabled") { value(false) }
                    jsonPath("$.rentalEnabled") { value(true) }
                }

                verify(exactly = 1) { updateNotificationPreferenceUseCase.execute(any()) }
            }
        }

        When("모든 알림 설정을 한 번에 업데이트하면") {
            Then("200 OK와 전체 업데이트된 NotificationPreferenceResult가 반환된다") {
                val fullyUpdatedResult = NotificationPreferenceResult(
                    userId = 42L,
                    chatEnabled = false,
                    rentalEnabled = false,
                    settlementEnabled = true,
                    marketingEnabled = true,
                )
                every { updateNotificationPreferenceUseCase.execute(any()) } returns fullyUpdatedResult

                val requestBody = mapOf(
                    "chatEnabled" to false,
                    "rentalEnabled" to false,
                    "settlementEnabled" to true,
                    "marketingEnabled" to true,
                )

                val result = mockMvc.patch("/api/v1/me/notification-preferences") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requestBody)
                    header(HEADER_USER_ID, "42")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.chatEnabled") { value(false) }
                    jsonPath("$.rentalEnabled") { value(false) }
                    jsonPath("$.settlementEnabled") { value(true) }
                    jsonPath("$.marketingEnabled") { value(true) }
                }
            }
        }

        When("빈 바디로 PATCH하면") {
            Then("200 OK가 반환된다 — 모든 필드가 null이면 변경 없음 처리") {
                every { updateNotificationPreferenceUseCase.execute(any()) } returns samplePreferenceResult

                val result = mockMvc.patch("/api/v1/me/notification-preferences") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(emptyMap<String, Any>())
                    header(HEADER_USER_ID, "42")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.userId") { value(42) }
                }
            }
        }

        When("X-Member-Id 헤더가 없으면") {
            Then("401 UNAUTHORIZED가 반환된다") {
                val requestBody = mapOf("chatEnabled" to false)

                val result = mockMvc.patch("/api/v1/me/notification-preferences") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requestBody)
                }

                result.andExpect {
                    status { isUnauthorized() }
                }
            }
        }
    }
})
