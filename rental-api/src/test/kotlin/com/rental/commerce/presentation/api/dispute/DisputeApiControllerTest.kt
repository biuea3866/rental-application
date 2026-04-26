package com.rental.commerce.presentation.api.dispute

import com.fasterxml.jackson.databind.ObjectMapper
import com.rental.commerce.application.dispute.CancelDisputeUseCase
import com.rental.commerce.application.dispute.DisputeResult
import com.rental.commerce.application.dispute.GetDisputeUseCase
import com.rental.commerce.application.dispute.OpenDisputeUseCase
import com.rental.commerce.domain.dispute.DisputeForbiddenException
import com.rental.commerce.domain.dispute.DisputeNotFoundException
import com.rental.commerce.domain.dispute.DisputeReason
import com.rental.commerce.domain.dispute.DisputeStatus
import com.rental.commerce.presentation.api.common.AuthenticatedRequestWrapper.Companion.HEADER_USER_ID
import com.rental.commerce.presentation.api.common.AuthenticatedRequestWrapper.Companion.HEADER_USER_ROLE
import com.rental.commerce.presentation.api.common.GlobalExceptionHandler
import com.rental.commerce.presentation.api.common.MemberIdArgumentResolver
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.time.ZonedDateTime

class DisputeApiControllerTest : BehaviorSpec({

    val openDisputeUseCase = mockk<OpenDisputeUseCase>()
    val getDisputeUseCase = mockk<GetDisputeUseCase>()
    val cancelDisputeUseCase = mockk<CancelDisputeUseCase>()

    val controller = DisputeApiController(
        openDisputeUseCase = openDisputeUseCase,
        getDisputeUseCase = getDisputeUseCase,
        cancelDisputeUseCase = cancelDisputeUseCase,
    )

    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setCustomArgumentResolvers(MemberIdArgumentResolver())
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    val objectMapper = ObjectMapper().apply {
        findAndRegisterModules()
    }

    val now = ZonedDateTime.now()
    val sampleDisputeResult = DisputeResult(
        id = 1L,
        rentalId = 100L,
        openerId = 42L,
        reason = DisputeReason.DAMAGED,
        description = "상품이 파손된 채로 반납되었습니다",
        status = DisputeStatus.OPEN,
        refundAmount = null,
        resolvedAt = null,
        createdAt = now,
    )

    beforeEach {
        clearMocks(openDisputeUseCase, getDisputeUseCase, cancelDisputeUseCase)
    }

    Given("POST /api/v1/disputes") {

        When("정상적인 분쟁 오픈 요청이면") {
            Then("201 CREATED와 DisputeResult가 반환된다") {
                val requestBody = mapOf(
                    "rentalId" to 100L,
                    "reason" to "DAMAGED",
                    "description" to "상품이 파손된 채로 반납되었습니다",
                    "attachmentUrls" to emptyList<String>(),
                )
                every { openDisputeUseCase.execute(any()) } returns sampleDisputeResult

                val result = mockMvc.post("/api/v1/disputes") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requestBody)
                    header(HEADER_USER_ID, "42")
                }

                result.andExpect {
                    status { isCreated() }
                    jsonPath("$.id") { value(1) }
                    jsonPath("$.rentalId") { value(100) }
                    jsonPath("$.openerId") { value(42) }
                    jsonPath("$.reason") { value("DAMAGED") }
                    jsonPath("$.status") { value("OPEN") }
                }

                verify(exactly = 1) { openDisputeUseCase.execute(any()) }
            }
        }

        When("description이 blank이면") {
            Then("400 INVALID_INPUT이 반환된다") {
                val requestBody = mapOf(
                    "rentalId" to 100L,
                    "reason" to "DAMAGED",
                    "description" to "",
                    "attachmentUrls" to emptyList<String>(),
                )

                val result = mockMvc.post("/api/v1/disputes") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requestBody)
                    header(HEADER_USER_ID, "42")
                }

                result.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("INVALID_INPUT") }
                }
            }
        }

        When("X-Member-Id 헤더가 없으면") {
            Then("401 UNAUTHORIZED가 반환된다") {
                val requestBody = mapOf(
                    "rentalId" to 100L,
                    "reason" to "DAMAGED",
                    "description" to "상품이 파손된 채로 반납되었습니다",
                )

                val result = mockMvc.post("/api/v1/disputes") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requestBody)
                }

                result.andExpect {
                    status { isUnauthorized() }
                }
            }
        }
    }

    Given("GET /api/v1/disputes/{id}") {

        When("당사자가 정상적으로 조회하면") {
            Then("200 OK와 DisputeResult가 반환된다") {
                every { getDisputeUseCase.execute(disputeId = 1L, requesterId = 42L, isAdmin = false) } returns sampleDisputeResult

                val result = mockMvc.get("/api/v1/disputes/1") {
                    header(HEADER_USER_ID, "42")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.id") { value(1) }
                    jsonPath("$.rentalId") { value(100) }
                    jsonPath("$.status") { value("OPEN") }
                }
            }
        }

        When("관리자가 X-Member-Role=ADMIN 헤더로 조회하면") {
            Then("200 OK와 DisputeResult가 반환된다") {
                every { getDisputeUseCase.execute(disputeId = 1L, requesterId = 99L, isAdmin = true) } returns sampleDisputeResult

                val result = mockMvc.get("/api/v1/disputes/1") {
                    header(HEADER_USER_ID, "99")
                    header(HEADER_USER_ROLE, "ADMIN")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.id") { value(1) }
                }
            }
        }

        When("접근 권한이 없는 사용자가 조회하면") {
            Then("403 DISPUTE_FORBIDDEN이 반환된다") {
                every {
                    getDisputeUseCase.execute(disputeId = 1L, requesterId = 999L, isAdmin = false)
                } throws DisputeForbiddenException()

                val result = mockMvc.get("/api/v1/disputes/1") {
                    header(HEADER_USER_ID, "999")
                }

                result.andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("DISPUTE_FORBIDDEN") }
                }
            }
        }

        When("존재하지 않는 분쟁을 조회하면") {
            Then("404 DISPUTE_NOT_FOUND가 반환된다") {
                every {
                    getDisputeUseCase.execute(disputeId = 999L, requesterId = 42L, isAdmin = false)
                } throws DisputeNotFoundException()

                val result = mockMvc.get("/api/v1/disputes/999") {
                    header(HEADER_USER_ID, "42")
                }

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("DISPUTE_NOT_FOUND") }
                }
            }
        }
    }

    Given("POST /api/v1/disputes/{id}/cancel") {

        When("오픈자가 OPEN 상태의 분쟁을 취소하면") {
            Then("200 OK와 CANCELLED 상태의 DisputeResult가 반환된다") {
                val cancelledResult = sampleDisputeResult.copy(status = DisputeStatus.CANCELLED)
                every { cancelDisputeUseCase.execute(disputeId = 1L, requesterId = 42L) } returns cancelledResult

                val result = mockMvc.post("/api/v1/disputes/1/cancel") {
                    header(HEADER_USER_ID, "42")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.status") { value("CANCELLED") }
                }

                verify(exactly = 1) { cancelDisputeUseCase.execute(disputeId = 1L, requesterId = 42L) }
            }
        }

        When("OPEN 상태가 아닌 분쟁을 취소하면") {
            Then("400 INVALID_STATE_TRANSITION이 반환된다") {
                every {
                    cancelDisputeUseCase.execute(disputeId = 2L, requesterId = 42L)
                } throws IllegalStateException("취소는 OPEN 상태에서만 가능합니다. current=UNDER_REVIEW")

                val result = mockMvc.post("/api/v1/disputes/2/cancel") {
                    header(HEADER_USER_ID, "42")
                }

                result.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("INVALID_STATE_TRANSITION") }
                }
            }
        }

        When("권한 없는 사용자가 취소하면") {
            Then("403 DISPUTE_FORBIDDEN이 반환된다") {
                every {
                    cancelDisputeUseCase.execute(disputeId = 1L, requesterId = 999L)
                } throws DisputeForbiddenException()

                val result = mockMvc.post("/api/v1/disputes/1/cancel") {
                    header(HEADER_USER_ID, "999")
                }

                result.andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("DISPUTE_FORBIDDEN") }
                }
            }
        }

        When("존재하지 않는 분쟁을 취소하면") {
            Then("404 DISPUTE_NOT_FOUND가 반환된다") {
                every {
                    cancelDisputeUseCase.execute(disputeId = 999L, requesterId = 42L)
                } throws DisputeNotFoundException()

                val result = mockMvc.post("/api/v1/disputes/999/cancel") {
                    header(HEADER_USER_ID, "42")
                }

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("DISPUTE_NOT_FOUND") }
                }
            }
        }
    }
})
