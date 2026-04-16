package com.rental.commerce.presentation.api.rental

import com.rental.commerce.application.rental.ApproveRentalUseCase
import com.rental.commerce.application.rental.CancelRentalUseCase
import com.rental.commerce.application.rental.GetMyRentalsUseCase
import com.rental.commerce.application.rental.GetRentalDetailUseCase
import com.rental.commerce.application.rental.ProcessPaymentResult
import com.rental.commerce.application.rental.ProcessPaymentUseCase
import com.rental.commerce.application.rental.RejectRentalUseCase
import com.rental.commerce.application.rental.RentalDetailResult
import com.rental.commerce.application.rental.RentalSummaryResult
import com.rental.commerce.application.rental.RequestRentalResult
import com.rental.commerce.application.rental.RequestRentalUseCase
import com.rental.commerce.application.rental.ReturnRentalUseCase
import com.rental.commerce.application.rental.StartRentalUseCase
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.PaymentMethod
import com.rental.commerce.domain.rental.PaymentStatus
import com.rental.commerce.domain.rental.RentalStatus
import com.rental.commerce.presentation.api.common.AuthenticatedRequestWrapper
import com.rental.commerce.presentation.api.common.GlobalExceptionHandler
import com.rental.commerce.presentation.api.common.MemberIdArgumentResolver
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class RentalApiControllerTest : BehaviorSpec({

    val requestRentalUseCase = mockk<RequestRentalUseCase>()
    val approveRentalUseCase = mockk<ApproveRentalUseCase>()
    val rejectRentalUseCase = mockk<RejectRentalUseCase>()
    val processPaymentUseCase = mockk<ProcessPaymentUseCase>()
    val startRentalUseCase = mockk<StartRentalUseCase>()
    val returnRentalUseCase = mockk<ReturnRentalUseCase>()
    val cancelRentalUseCase = mockk<CancelRentalUseCase>()
    val getMyRentalsUseCase = mockk<GetMyRentalsUseCase>()
    val getRentalDetailUseCase = mockk<GetRentalDetailUseCase>()

    val controller = RentalApiController(
        requestRentalUseCase = requestRentalUseCase,
        approveRentalUseCase = approveRentalUseCase,
        rejectRentalUseCase = rejectRentalUseCase,
        processPaymentUseCase = processPaymentUseCase,
        startRentalUseCase = startRentalUseCase,
        returnRentalUseCase = returnRentalUseCase,
        cancelRentalUseCase = cancelRentalUseCase,
        getMyRentalsUseCase = getMyRentalsUseCase,
        getRentalDetailUseCase = getRentalDetailUseCase,
    )

    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setCustomArgumentResolvers(MemberIdArgumentResolver())
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    beforeEach {
        clearMocks(
            requestRentalUseCase,
            approveRentalUseCase,
            rejectRentalUseCase,
            processPaymentUseCase,
            startRentalUseCase,
            returnRentalUseCase,
            cancelRentalUseCase,
            getMyRentalsUseCase,
            getRentalDetailUseCase,
        )
    }

    // ──────────────────────────────────────────────────────────
    // POST /api/v1/rentals — 대여 신청
    // ──────────────────────────────────────────────────────────
    Given("POST /api/v1/rentals") {

        When("정상적인 대여 신청 요청을 보내면") {
            Then("201 Created와 대여 정보가 반환된다") {
                val now = ZonedDateTime.now()
                val response = RequestRentalResult(
                    rentalId = 1001L,
                    productId = 42L,
                    status = RentalStatus.REQUESTED,
                    startDate = now.plusDays(5),
                    endDate = now.plusDays(12),
                    totalAmount = 120000L,
                    depositAmount = 50000L,
                    requestedAt = now,
                )

                every { requestRentalUseCase.execute(any()) } returns response

                val result = mockMvc.post("/api/v1/rentals") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """
                        {
                          "productId": 42,
                          "startDate": "2026-05-01T00:00:00+09:00",
                          "endDate": "2026-05-07T00:00:00+09:00",
                          "dailyPrice": 10000,
                          "deliveryInfo": {
                            "recipientName": "홍길동",
                            "recipientPhone": "010-1234-5678",
                            "addressLine1": "서울특별시 강남구 테헤란로 123",
                            "addressLine2": "101호",
                            "zipCode": "06234"
                          }
                        }
                    """.trimIndent()
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isCreated() }
                    jsonPath("$.rentalId") { value(1001) }
                    jsonPath("$.productId") { value(42) }
                    jsonPath("$.status") { value("REQUESTED") }
                    jsonPath("$.totalAmount") { value(120000) }
                    jsonPath("$.depositAmount") { value(50000) }
                }
            }
        }

        When("상품을 찾을 수 없는 경우") {
            Then("404 RESOURCE_NOT_FOUND 에러가 반환된다") {
                every { requestRentalUseCase.execute(any()) } throws ResourceNotFoundException(
                    errorCode = ErrorCode.RESOURCE_NOT_FOUND,
                    message = "상품을 찾을 수 없습니다.",
                )

                val result = mockMvc.post("/api/v1/rentals") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """
                        {
                          "productId": 999,
                          "startDate": "2026-05-01T00:00:00+09:00",
                          "endDate": "2026-05-07T00:00:00+09:00",
                          "dailyPrice": 10000,
                          "deliveryInfo": {
                            "recipientName": "홍길동",
                            "recipientPhone": "010-1234-5678",
                            "addressLine1": "서울특별시 강남구 테헤란로 123",
                            "zipCode": "06234"
                          }
                        }
                    """.trimIndent()
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("RESOURCE_NOT_FOUND") }
                }
            }
        }

        When("대여 기간 중복 충돌이 발생하면") {
            Then("409 RENTAL_PERIOD_CONFLICT 에러가 반환된다") {
                every { requestRentalUseCase.execute(any()) } throws BusinessException(
                    errorCode = ErrorCode.RENTAL_PERIOD_CONFLICT,
                )

                val result = mockMvc.post("/api/v1/rentals") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """
                        {
                          "productId": 42,
                          "startDate": "2026-05-01T00:00:00+09:00",
                          "endDate": "2026-05-07T00:00:00+09:00",
                          "dailyPrice": 10000,
                          "deliveryInfo": {
                            "recipientName": "홍길동",
                            "recipientPhone": "010-1234-5678",
                            "addressLine1": "서울특별시 강남구 테헤란로 123",
                            "zipCode": "06234"
                          }
                        }
                    """.trimIndent()
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isConflict() }
                    jsonPath("$.code") { value("RENTAL_PERIOD_CONFLICT") }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // PATCH /api/v1/rentals/{id}/approve — 승인
    // ──────────────────────────────────────────────────────────
    Given("PATCH /api/v1/rentals/{id}/approve") {

        When("등록자가 정상적으로 승인하면") {
            Then("200 OK가 반환된다") {
                justRun { approveRentalUseCase.execute(any()) }

                val result = mockMvc.patch("/api/v1/rentals/1001/approve") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "2")
                }

                result.andExpect {
                    status { isOk() }
                }

                verify { approveRentalUseCase.execute(any()) }
            }
        }

        When("권한 없는 사용자가 승인 요청하면") {
            Then("403 FORBIDDEN 에러가 반환된다") {
                every { approveRentalUseCase.execute(any()) } throws BusinessException(
                    errorCode = ErrorCode.FORBIDDEN,
                )

                val result = mockMvc.patch("/api/v1/rentals/1001/approve") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "99")
                }

                result.andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("FORBIDDEN") }
                }
            }
        }

        When("존재하지 않는 대여를 승인하면") {
            Then("404 RENTAL_NOT_FOUND 에러가 반환된다") {
                every { approveRentalUseCase.execute(any()) } throws ResourceNotFoundException(
                    errorCode = ErrorCode.RENTAL_NOT_FOUND,
                )

                val result = mockMvc.patch("/api/v1/rentals/9999/approve") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "2")
                }

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("RENTAL_NOT_FOUND") }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // PATCH /api/v1/rentals/{id}/reject — 거절
    // ──────────────────────────────────────────────────────────
    Given("PATCH /api/v1/rentals/{id}/reject") {

        When("등록자가 reason을 포함하여 거절하면") {
            Then("200 OK가 반환된다") {
                justRun { rejectRentalUseCase.execute(any()) }

                val result = mockMvc.patch("/api/v1/rentals/1001/reject") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"reason": "해당 기간에 다른 일정이 생겼습니다."}"""
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "2")
                }

                result.andExpect {
                    status { isOk() }
                }

                verify { rejectRentalUseCase.execute(any()) }
            }
        }

        When("reason이 누락된 경우") {
            Then("400 Bad Request가 반환된다") {
                val result = mockMvc.patch("/api/v1/rentals/1001/reject") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{}"""
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "2")
                }

                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }

        When("권한 없는 사용자가 거절하면") {
            Then("403 FORBIDDEN 에러가 반환된다") {
                every { rejectRentalUseCase.execute(any()) } throws BusinessException(
                    errorCode = ErrorCode.FORBIDDEN,
                )

                val result = mockMvc.patch("/api/v1/rentals/1001/reject") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"reason": "거절합니다."}"""
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "99")
                }

                result.andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("FORBIDDEN") }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // POST /api/v1/rentals/{id}/payment — 결제
    // ──────────────────────────────────────────────────────────
    Given("POST /api/v1/rentals/{id}/payment") {

        When("대여자가 정상적으로 결제하면") {
            Then("200 OK와 결제 결과가 반환된다") {
                val response = ProcessPaymentResult(
                    rentalId = 1001L,
                    paymentId = 5001L,
                    rentalStatus = RentalStatus.PAID,
                    paymentStatus = PaymentStatus.COMPLETED,
                    externalPaymentId = "toss-key-abc",
                )

                every { processPaymentUseCase.execute(any()) } returns response

                val result = mockMvc.post("/api/v1/rentals/1001/payment") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """
                        {
                          "paymentKey": "toss-key-abc",
                          "orderId": "RC-1001-123456",
                          "amount": 120000,
                          "paymentMethod": "CARD"
                        }
                    """.trimIndent()
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.rentalId") { value(1001) }
                    jsonPath("$.paymentId") { value(5001) }
                    jsonPath("$.rentalStatus") { value("PAID") }
                    jsonPath("$.paymentStatus") { value("COMPLETED") }
                }
            }
        }

        When("결제가 실패하면") {
            Then("402 PAYMENT_FAILED 에러가 반환된다") {
                every { processPaymentUseCase.execute(any()) } throws BusinessException(
                    errorCode = ErrorCode.PAYMENT_FAILED,
                )

                val result = mockMvc.post("/api/v1/rentals/1001/payment") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """
                        {
                          "paymentKey": "invalid-key",
                          "orderId": "RC-1001-123456",
                          "amount": 120000,
                          "paymentMethod": "CARD"
                        }
                    """.trimIndent()
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isPaymentRequired() }
                    jsonPath("$.code") { value("PAYMENT_FAILED") }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // PATCH /api/v1/rentals/{id}/start — 대여 시작
    // ──────────────────────────────────────────────────────────
    Given("PATCH /api/v1/rentals/{id}/start") {

        When("등록자가 정상적으로 대여 시작 처리하면") {
            Then("200 OK가 반환된다") {
                justRun { startRentalUseCase.execute(any()) }

                val result = mockMvc.patch("/api/v1/rentals/1001/start") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "2")
                }

                result.andExpect {
                    status { isOk() }
                }

                verify { startRentalUseCase.execute(any()) }
            }
        }

        When("권한 없는 사용자가 대여 시작을 요청하면") {
            Then("403 FORBIDDEN 에러가 반환된다") {
                every { startRentalUseCase.execute(any()) } throws BusinessException(
                    errorCode = ErrorCode.FORBIDDEN,
                )

                val result = mockMvc.patch("/api/v1/rentals/1001/start") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "99")
                }

                result.andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("FORBIDDEN") }
                }
            }
        }

        When("PAID 상태가 아닌 대여를 시작하면") {
            Then("400 INVALID_STATE_TRANSITION 에러가 반환된다") {
                every { startRentalUseCase.execute(any()) } throws InvalidStateTransitionException(
                    "REQUESTED에서 IN_USE(으)로 전이할 수 없습니다",
                )

                val result = mockMvc.patch("/api/v1/rentals/1001/start") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "2")
                }

                result.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("INVALID_STATE_TRANSITION") }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // PATCH /api/v1/rentals/{id}/return — 반납
    // ──────────────────────────────────────────────────────────
    Given("PATCH /api/v1/rentals/{id}/return") {

        When("대여자가 정상적으로 반납 처리하면") {
            Then("200 OK가 반환된다") {
                justRun { returnRentalUseCase.execute(any()) }

                val result = mockMvc.patch("/api/v1/rentals/1001/return") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                }

                verify { returnRentalUseCase.execute(any()) }
            }
        }

        When("IN_USE 상태가 아닌 대여를 반납하면") {
            Then("400 INVALID_STATE_TRANSITION 에러가 반환된다") {
                every { returnRentalUseCase.execute(any()) } throws InvalidStateTransitionException(
                    "REQUESTED에서 RETURNED(으)로 전이할 수 없습니다",
                )

                val result = mockMvc.patch("/api/v1/rentals/1001/return") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("INVALID_STATE_TRANSITION") }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // DELETE /api/v1/rentals/{id} — 취소
    // ──────────────────────────────────────────────────────────
    Given("DELETE /api/v1/rentals/{id}") {

        When("대여자가 reason을 포함하여 취소하면") {
            Then("200 OK가 반환된다") {
                justRun { cancelRentalUseCase.execute(any()) }

                val result = mockMvc.delete("/api/v1/rentals/1001") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"reason": "일정이 변경되었습니다."}"""
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                }

                verify { cancelRentalUseCase.execute(any()) }
            }
        }

        When("reason이 누락된 경우") {
            Then("400 Bad Request가 반환된다") {
                val result = mockMvc.delete("/api/v1/rentals/1001") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{}"""
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }

        When("권한 없는 사용자가 취소하면") {
            Then("403 FORBIDDEN 에러가 반환된다") {
                every { cancelRentalUseCase.execute(any()) } throws BusinessException(
                    errorCode = ErrorCode.FORBIDDEN,
                )

                val result = mockMvc.delete("/api/v1/rentals/1001") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"reason": "취소합니다."}"""
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "99")
                }

                result.andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("FORBIDDEN") }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // GET /api/v1/my-rentals — 내 대여 목록
    // ──────────────────────────────────────────────────────────
    Given("GET /api/v1/my-rentals") {

        When("정상적으로 내 대여 목록을 조회하면") {
            Then("200 OK와 대여 목록이 반환된다") {
                val now = ZonedDateTime.now()
                val summaries = listOf(
                    RentalSummaryResult(
                        rentalId = 1001L,
                        productId = 42L,
                        status = RentalStatus.IN_USE,
                        startDate = now.plusDays(1),
                        endDate = now.plusDays(8),
                        totalAmount = 120000L,
                        depositAmount = 50000L,
                        requestedAt = now,
                    )
                )

                every { getMyRentalsUseCase.execute(any()) } returns summaries

                val result = mockMvc.get("/api/v1/my-rentals") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.rentals.length()") { value(1) }
                    jsonPath("$.rentals[0].rentalId") { value(1001) }
                    jsonPath("$.rentals[0].productId") { value(42) }
                    jsonPath("$.rentals[0].status") { value("IN_USE") }
                }
            }
        }

        When("대여 내역이 없는 경우") {
            Then("200 OK와 빈 목록이 반환된다") {
                every { getMyRentalsUseCase.execute(any()) } returns emptyList()

                val result = mockMvc.get("/api/v1/my-rentals") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.rentals.length()") { value(0) }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // GET /api/v1/rentals/{id} — 대여 상세
    // ──────────────────────────────────────────────────────────
    Given("GET /api/v1/rentals/{id}") {

        When("참여자가 정상적으로 대여 상세를 조회하면") {
            Then("200 OK와 대여 상세 정보가 반환된다") {
                val now = ZonedDateTime.now()
                val detail = RentalDetailResult(
                    rentalId = 1001L,
                    renterId = 1L,
                    lenderId = 2L,
                    productId = 42L,
                    status = RentalStatus.IN_USE,
                    startDate = now.plusDays(1),
                    endDate = now.plusDays(8),
                    totalAmount = 120000L,
                    depositAmount = 50000L,
                    deliveryInfo = DeliveryInfo(
                        recipientName = "홍길동",
                        recipientPhone = "010-1234-5678",
                        addressLine1 = "서울특별시 강남구 테헤란로 123",
                        addressLine2 = "101호",
                        zipCode = "06234",
                    ),
                    cancelReason = null,
                    requestedAt = now,
                    approvedAt = now.plusHours(1),
                    paidAt = now.plusHours(2),
                    startedAt = now.plusDays(1),
                    returnedAt = null,
                    cancelledAt = null,
                )

                every { getRentalDetailUseCase.execute(any()) } returns detail

                val result = mockMvc.get("/api/v1/rentals/1001") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.rentalId") { value(1001) }
                    jsonPath("$.renterId") { value(1) }
                    jsonPath("$.lenderId") { value(2) }
                    jsonPath("$.productId") { value(42) }
                    jsonPath("$.status") { value("IN_USE") }
                    jsonPath("$.totalAmount") { value(120000) }
                    jsonPath("$.depositAmount") { value(50000) }
                    jsonPath("$.deliveryInfo.recipientName") { value("홍길동") }
                    jsonPath("$.deliveryInfo.zipCode") { value("06234") }
                }
            }
        }

        When("참여자가 아닌 사용자가 대여 상세를 조회하면") {
            Then("403 FORBIDDEN 에러가 반환된다") {
                every { getRentalDetailUseCase.execute(any()) } throws BusinessException(
                    errorCode = ErrorCode.FORBIDDEN,
                )

                val result = mockMvc.get("/api/v1/rentals/1001") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "99")
                }

                result.andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("FORBIDDEN") }
                }
            }
        }

        When("존재하지 않는 대여를 조회하면") {
            Then("404 RENTAL_NOT_FOUND 에러가 반환된다") {
                every { getRentalDetailUseCase.execute(any()) } throws ResourceNotFoundException(
                    errorCode = ErrorCode.RENTAL_NOT_FOUND,
                )

                val result = mockMvc.get("/api/v1/rentals/9999") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("RENTAL_NOT_FOUND") }
                }
            }
        }
    }
})
