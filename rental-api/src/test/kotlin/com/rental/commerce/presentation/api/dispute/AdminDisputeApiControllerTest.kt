package com.rental.commerce.presentation.api.dispute

import com.fasterxml.jackson.databind.ObjectMapper
import com.rental.commerce.application.dispute.DisputeResult
import com.rental.commerce.application.dispute.ResolveDisputeCommand
import com.rental.commerce.application.dispute.ResolveDisputeUseCase
import com.rental.commerce.application.dispute.StartDisputeReviewUseCase
import com.rental.commerce.domain.dispute.DisputeNotFoundException
import com.rental.commerce.domain.dispute.DisputeReason
import com.rental.commerce.domain.dispute.DisputeStatus
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
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.time.ZonedDateTime

class AdminDisputeApiControllerTest : BehaviorSpec({

    val startDisputeReviewUseCase = mockk<StartDisputeReviewUseCase>()
    val resolveDisputeUseCase = mockk<ResolveDisputeUseCase>()

    val controller = AdminDisputeApiController(
        startDisputeReviewUseCase = startDisputeReviewUseCase,
        resolveDisputeUseCase = resolveDisputeUseCase,
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
    val openDisputeResult = DisputeResult(
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
        clearMocks(startDisputeReviewUseCase, resolveDisputeUseCase)
    }

    Given("PATCH /api/v1/admin/disputes/{id}/review") {

        When("관리자가 OPEN 상태의 분쟁을 검토 시작하면") {
            Then("200 OK와 UNDER_REVIEW 상태의 DisputeResult가 반환된다") {
                val underReviewResult = openDisputeResult.copy(status = DisputeStatus.UNDER_REVIEW)
                every { startDisputeReviewUseCase.execute(1L) } returns underReviewResult

                val result = mockMvc.patch("/api/v1/admin/disputes/1/review") {
                    header(HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.id") { value(1) }
                    jsonPath("$.status") { value("UNDER_REVIEW") }
                }

                verify(exactly = 1) { startDisputeReviewUseCase.execute(1L) }
            }
        }

        When("OPEN 상태가 아닌 분쟁에 검토 시작을 요청하면") {
            Then("400 INVALID_STATE_TRANSITION이 반환된다") {
                every {
                    startDisputeReviewUseCase.execute(2L)
                } throws IllegalStateException("검토 시작은 OPEN 상태에서만 가능합니다. current=UNDER_REVIEW")

                val result = mockMvc.patch("/api/v1/admin/disputes/2/review") {
                    header(HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("INVALID_STATE_TRANSITION") }
                }
            }
        }

        When("존재하지 않는 분쟁에 검토 시작을 요청하면") {
            Then("404 DISPUTE_NOT_FOUND가 반환된다") {
                every {
                    startDisputeReviewUseCase.execute(999L)
                } throws DisputeNotFoundException()

                val result = mockMvc.patch("/api/v1/admin/disputes/999/review") {
                    header(HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("DISPUTE_NOT_FOUND") }
                }
            }
        }
    }

    Given("POST /api/v1/admin/disputes/{id}/resolve — FULL_REFUND") {

        When("전액 환불로 분쟁을 해결하면") {
            Then("200 OK와 RESOLVED_REFUND 상태의 DisputeResult가 반환된다") {
                val resolvedResult = openDisputeResult.copy(
                    status = DisputeStatus.RESOLVED_REFUND,
                    refundAmount = BigDecimal("50000"),
                    resolvedAt = now,
                )
                every { resolveDisputeUseCase.execute(any()) } returns resolvedResult

                val requestBody = mapOf(
                    "type" to "FULL_REFUND",
                    "refundAmount" to 50000,
                )

                val result = mockMvc.post("/api/v1/admin/disputes/1/resolve") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requestBody)
                    header(HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.status") { value("RESOLVED_REFUND") }
                    jsonPath("$.refundAmount") { value(50000) }
                }

                verify(exactly = 1) { resolveDisputeUseCase.execute(any()) }
            }
        }

        When("FULL_REFUND인데 refundAmount가 없으면") {
            Then("400 INVALID_INPUT이 반환된다 — ResolveDisputeCommand init에서 IllegalArgumentException") {
                every {
                    resolveDisputeUseCase.execute(any())
                } throws IllegalArgumentException("FULL_REFUND 해결 시 refundAmount 는 0보다 커야 합니다")

                val requestBody = mapOf(
                    "type" to "FULL_REFUND",
                )

                val result = mockMvc.post("/api/v1/admin/disputes/1/resolve") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requestBody)
                    header(HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("INVALID_INPUT") }
                }
            }
        }
    }

    Given("POST /api/v1/admin/disputes/{id}/resolve — PARTIAL") {

        When("부분 환불로 분쟁을 해결하면") {
            Then("200 OK와 RESOLVED_PARTIAL 상태의 DisputeResult가 반환된다") {
                val resolvedResult = openDisputeResult.copy(
                    status = DisputeStatus.RESOLVED_PARTIAL,
                    refundAmount = BigDecimal("20000"),
                    resolvedAt = now,
                )
                every { resolveDisputeUseCase.execute(any()) } returns resolvedResult

                val requestBody = mapOf(
                    "type" to "PARTIAL",
                    "refundAmount" to 20000,
                )

                val result = mockMvc.post("/api/v1/admin/disputes/1/resolve") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requestBody)
                    header(HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.status") { value("RESOLVED_PARTIAL") }
                    jsonPath("$.refundAmount") { value(20000) }
                }
            }
        }
    }

    Given("POST /api/v1/admin/disputes/{id}/resolve — REJECTED") {

        When("분쟁을 기각하면") {
            Then("200 OK와 RESOLVED_REJECTED 상태의 DisputeResult가 반환된다") {
                val resolvedResult = openDisputeResult.copy(
                    status = DisputeStatus.RESOLVED_REJECTED,
                    resolvedAt = now,
                )
                every { resolveDisputeUseCase.execute(any()) } returns resolvedResult

                val requestBody = mapOf(
                    "type" to "REJECTED",
                )

                val result = mockMvc.post("/api/v1/admin/disputes/1/resolve") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requestBody)
                    header(HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.status") { value("RESOLVED_REJECTED") }
                }
            }
        }

        When("UNDER_REVIEW 상태가 아닌 분쟁에 resolve를 요청하면") {
            Then("400 INVALID_STATE_TRANSITION이 반환된다") {
                every {
                    resolveDisputeUseCase.execute(any())
                } throws IllegalStateException("해결은 UNDER_REVIEW 상태에서만 가능합니다. current=OPEN")

                val requestBody = mapOf(
                    "type" to "REJECTED",
                )

                val result = mockMvc.post("/api/v1/admin/disputes/3/resolve") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requestBody)
                    header(HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("INVALID_STATE_TRANSITION") }
                }
            }
        }

        When("존재하지 않는 분쟁을 resolve하면") {
            Then("404 DISPUTE_NOT_FOUND가 반환된다") {
                every {
                    resolveDisputeUseCase.execute(any())
                } throws DisputeNotFoundException()

                val requestBody = mapOf(
                    "type" to "REJECTED",
                )

                val result = mockMvc.post("/api/v1/admin/disputes/999/resolve") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requestBody)
                    header(HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("DISPUTE_NOT_FOUND") }
                }
            }
        }
    }
})
