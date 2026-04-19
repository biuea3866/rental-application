package com.rental.commerce.presentation.api.admin

import com.rental.commerce.application.admin.ActivateUserUseCase
import com.rental.commerce.application.admin.AdminDashboardQueryUseCase
import com.rental.commerce.application.admin.AdminDashboardResult
import com.rental.commerce.application.admin.AdminRentalPageResponse
import com.rental.commerce.application.admin.AdminRentalResponse
import com.rental.commerce.application.admin.GetAdminRentalsUseCase
import com.rental.commerce.application.admin.SuspendUserUseCase
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.rental.RentalStatus
import com.rental.commerce.presentation.api.common.GlobalExceptionHandler
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class AdminApiControllerTest : BehaviorSpec({

    val adminDashboardQueryUseCase = mockk<AdminDashboardQueryUseCase>()
    val getAdminRentalsUseCase = mockk<GetAdminRentalsUseCase>()
    val suspendUserUseCase = mockk<SuspendUserUseCase>()
    val activateUserUseCase = mockk<ActivateUserUseCase>()

    val controller = AdminApiController(
        adminDashboardQueryUseCase = adminDashboardQueryUseCase,
        getAdminRentalsUseCase = getAdminRentalsUseCase,
        suspendUserUseCase = suspendUserUseCase,
        activateUserUseCase = activateUserUseCase,
    )

    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    beforeEach {
        clearMocks(adminDashboardQueryUseCase, getAdminRentalsUseCase, suspendUserUseCase, activateUserUseCase)
    }

    // ──────────────────────────────────────────────────────────
    // GET /api/v1/admin/dashboard — 대시보드 통계 조회
    // ──────────────────────────────────────────────────────────
    Given("GET /api/v1/admin/dashboard") {

        When("정상 요청이 오면") {
            Then("200 OK와 대시보드 통계가 반환된다") {
                val statusCounts = mapOf(
                    RentalStatus.REQUESTED to 5L,
                    RentalStatus.APPROVED to 3L,
                    RentalStatus.PAID to 2L,
                )
                every { adminDashboardQueryUseCase.execute() } returns AdminDashboardResult(
                    totalRentals = 10L,
                    statusCounts = statusCounts,
                    revenue = 500_000L,
                )

                val result = mockMvc.get("/api/v1/admin/dashboard")

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.totalRentals") { value(10) }
                    jsonPath("$.revenue") { value(500000) }
                }
                verify(exactly = 1) { adminDashboardQueryUseCase.execute() }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // GET /api/v1/admin/rentals — 전체 대여 목록 조회
    // ──────────────────────────────────────────────────────────
    Given("GET /api/v1/admin/rentals") {

        When("파라미터 없이 목록을 조회하면") {
            Then("200 OK와 대여 목록이 반환된다") {
                val now = ZonedDateTime.now()
                every { getAdminRentalsUseCase.execute(any()) } returns AdminRentalPageResponse(
                    content = listOf(
                        AdminRentalResponse(
                            rentalId = 1L,
                            renterId = 10L,
                            lenderId = 20L,
                            productId = 42L,
                            status = RentalStatus.REQUESTED,
                            totalAmount = 70_000L,
                            requestedAt = now,
                        ),
                    ),
                    totalElements = 1L,
                    totalPages = 1,
                )

                val result = mockMvc.get("/api/v1/admin/rentals")

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.content.length()") { value(1) }
                    jsonPath("$.content[0].rentalId") { value(1) }
                    jsonPath("$.content[0].status") { value("REQUESTED") }
                    jsonPath("$.totalElements") { value(1) }
                    jsonPath("$.totalPages") { value(1) }
                }
                verify(exactly = 1) { getAdminRentalsUseCase.execute(any()) }
            }
        }

        When("status=APPROVED 필터로 조회하면") {
            Then("200 OK와 필터링된 대여 목록이 반환된다") {
                val now = ZonedDateTime.now()
                every { getAdminRentalsUseCase.execute(any()) } returns AdminRentalPageResponse(
                    content = listOf(
                        AdminRentalResponse(
                            rentalId = 2L,
                            renterId = 11L,
                            lenderId = 21L,
                            productId = 43L,
                            status = RentalStatus.APPROVED,
                            totalAmount = 50_000L,
                            requestedAt = now,
                        ),
                    ),
                    totalElements = 1L,
                    totalPages = 1,
                )

                val result = mockMvc.get("/api/v1/admin/rentals") {
                    param("status", "APPROVED")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.content[0].status") { value("APPROVED") }
                }
                verify(exactly = 1) {
                    getAdminRentalsUseCase.execute(match { it.status == RentalStatus.APPROVED })
                }
            }
        }

        When("대여가 없는 경우") {
            Then("200 OK와 빈 목록이 반환된다") {
                every { getAdminRentalsUseCase.execute(any()) } returns AdminRentalPageResponse(
                    content = emptyList(),
                    totalElements = 0L,
                    totalPages = 0,
                )

                val result = mockMvc.get("/api/v1/admin/rentals")

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.content.length()") { value(0) }
                    jsonPath("$.totalElements") { value(0) }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // POST /api/v1/admin/users/{userId}/suspend — 사용자 정지
    // ──────────────────────────────────────────────────────────
    Given("POST /api/v1/admin/users/{userId}/suspend") {

        When("유효한 사용자를 정지하면") {
            Then("200 OK가 반환된다") {
                every { suspendUserUseCase.execute(any()) } just Runs

                val result = mockMvc.post("/api/v1/admin/users/1/suspend")

                result.andExpect {
                    status { isOk() }
                }
                verify(exactly = 1) { suspendUserUseCase.execute(any()) }
            }
        }

        When("존재하지 않는 사용자를 정지하려고 하면") {
            Then("404 USER_NOT_FOUND 에러가 반환된다") {
                every { suspendUserUseCase.execute(any()) } throws BusinessException(
                    errorCode = ErrorCode.USER_NOT_FOUND,
                    message = "사용자를 찾을 수 없습니다. userId=999",
                )

                val result = mockMvc.post("/api/v1/admin/users/999/suspend")

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("USER_NOT_FOUND") }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // POST /api/v1/admin/users/{userId}/activate — 사용자 활성화
    // ──────────────────────────────────────────────────────────
    Given("POST /api/v1/admin/users/{userId}/activate") {

        When("유효한 사용자를 활성화하면") {
            Then("200 OK가 반환된다") {
                every { activateUserUseCase.execute(any()) } just Runs

                val result = mockMvc.post("/api/v1/admin/users/1/activate")

                result.andExpect {
                    status { isOk() }
                }
                verify(exactly = 1) { activateUserUseCase.execute(any()) }
            }
        }

        When("존재하지 않는 사용자를 활성화하려고 하면") {
            Then("404 USER_NOT_FOUND 에러가 반환된다") {
                every { activateUserUseCase.execute(any()) } throws BusinessException(
                    errorCode = ErrorCode.USER_NOT_FOUND,
                    message = "사용자를 찾을 수 없습니다. userId=999",
                )

                val result = mockMvc.post("/api/v1/admin/users/999/activate")

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("USER_NOT_FOUND") }
                }
            }
        }
    }
})
