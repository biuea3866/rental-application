package com.rental.commerce.presentation.api.settlement

import com.rental.commerce.application.settlement.GetMySettlementsUseCase
import com.rental.commerce.application.settlement.SettlementResult
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.settlement.SettlementStatus
import com.rental.commerce.presentation.api.common.AuthenticatedRequestWrapper
import com.rental.commerce.presentation.api.common.GlobalExceptionHandler
import com.rental.commerce.presentation.api.common.MemberIdArgumentResolver
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import java.time.ZonedDateTime
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class SettlementApiControllerTest : BehaviorSpec({

    val getMySettlementsUseCase = mockk<GetMySettlementsUseCase>()

    val controller = SettlementApiController(
        getMySettlementsUseCase = getMySettlementsUseCase,
    )

    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setCustomArgumentResolvers(MemberIdArgumentResolver())
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    beforeEach {
        clearMocks(getMySettlementsUseCase)
    }

    // ──────────────────────────────────────────────────────────
    // GET /api/v1/my-settlements — 내 정산 목록
    // ──────────────────────────────────────────────────────────
    Given("GET /api/v1/my-settlements") {

        When("정상적으로 내 정산 목록을 조회하면") {
            Then("200 OK와 정산 목록이 반환된다") {
                val now = ZonedDateTime.now()
                val settlements = listOf(
                    SettlementResult(
                        settlementId = 1L,
                        lenderId = 10L,
                        rentalId = 100L,
                        amount = BigDecimal("50000.00"),
                        commission = BigDecimal("5000.00"),
                        netAmount = BigDecimal("45000.00"),
                        status = SettlementStatus.PENDING,
                        settledAt = null,
                    ),
                )

                every { getMySettlementsUseCase.execute(any()) } returns PageResult(
                    content = settlements,
                    totalElements = 1L,
                    totalPages = 1,
                )

                val result = mockMvc.get("/api/v1/my-settlements") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "10")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.content.length()") { value(1) }
                    jsonPath("$.content[0].settlementId") { value(1) }
                    jsonPath("$.content[0].lenderId") { value(10) }
                    jsonPath("$.content[0].rentalId") { value(100) }
                    jsonPath("$.content[0].netAmount") { value(45000.00) }
                    jsonPath("$.content[0].status") { value("PENDING") }
                    jsonPath("$.totalElements") { value(1) }
                    jsonPath("$.totalPages") { value(1) }
                }

                verify(exactly = 1) { getMySettlementsUseCase.execute(any()) }
            }
        }

        When("정산 내역이 없는 경우") {
            Then("200 OK와 빈 목록이 반환된다") {
                every { getMySettlementsUseCase.execute(any()) } returns PageResult(
                    content = emptyList(),
                    totalElements = 0L,
                    totalPages = 0,
                )

                val result = mockMvc.get("/api/v1/my-settlements") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "10")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.content.length()") { value(0) }
                    jsonPath("$.totalElements") { value(0) }
                }
            }
        }

        When("인증 헤더 없이 요청하면") {
            Then("400 또는 401이 반환된다") {
                val result = mockMvc.get("/api/v1/my-settlements")

                result.andExpect {
                    status { is4xxClientError() }
                }
            }
        }
    }
})
