package com.rental.commerce.presentation.api.rental

import com.rental.commerce.application.rental.ApproveRentalUseCase
import com.rental.commerce.application.rental.CancelRentalUseCase
import com.rental.commerce.application.rental.DeliveryInfoResponse
import com.rental.commerce.application.rental.GetMyRentalsUseCase
import com.rental.commerce.application.rental.GetRentalDetailUseCase
import com.rental.commerce.application.rental.ProcessPaymentResult
import com.rental.commerce.application.rental.ProcessPaymentUseCase
import com.rental.commerce.application.rental.RentalDetailResponse
import com.rental.commerce.application.rental.RentalResponse
import com.rental.commerce.application.rental.RentalSummaryResponse
import com.rental.commerce.application.rental.RejectRentalUseCase
import com.rental.commerce.application.rental.RequestRentalUseCase
import com.rental.commerce.application.rental.ReturnRentalUseCase
import com.rental.commerce.application.rental.StartRentalUseCase
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.rental.PaymentMethod
import com.rental.commerce.domain.rental.PaymentStatus
import com.rental.commerce.domain.rental.RentalStatus
import com.rental.commerce.presentation.api.common.AuthenticatedRequestWrapper
import com.rental.commerce.presentation.api.common.GlobalExceptionHandler
import com.rental.commerce.presentation.api.common.MemberIdArgumentResolver
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.ZonedDateTime

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

    beforeEach { clearAllMocks() }

    val now = ZonedDateTime.now()
    val deliveryInfoResp = DeliveryInfoResponse(
        recipientName = "홍길동",
        recipientPhone = "010-1234-5678",
        addressLine1 = "서울특별시 강남구 테헤란로 123",
        addressLine2 = "101호",
        zipCode = "06234",
    )

    fun rentalResponse(status: RentalStatus = RentalStatus.REQUESTED) = RentalResponse(
        rentalId = 1001L,
        renterId = 11L,
        lenderId = 22L,
        productId = 42L,
        status = status,
        startDate = now.plusDays(1),
        endDate = now.plusDays(7),
        totalAmount = 70000L,
        depositAmount = 50000L,
        orderId = "RC-42-1713063600000",
        cancelReason = null,
        requestedAt = now,
        approvedAt = null,
        paidAt = null,
        startedAt = null,
        returnedAt = null,
        cancelledAt = null,
        deliveryInfo = deliveryInfoResp,
    )

    Given("POST /api/v1/rentals") {

        When("정상적인 대여 신청 요청을 보내면") {
            Then("201 Created와 대여 정보가 반환된다") {
                every { requestRentalUseCase.execute(any()) } returns rentalResponse()

                val result = mockMvc.post("/api/v1/rentals") {
                    contentType = MediaType.APPLICATION_JSON
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "11")
                    content = """
                        {
                          "productId": 42,
                          "lenderId": 22,
                          "startDate": "2026-05-01T00:00:00+09:00",
                          "endDate": "2026-05-07T00:00:00+09:00",
                          "totalAmount": 70000,
                          "depositAmount": 50000,
                          "deliveryInfo": {
                            "recipientName": "홍길동",
                            "recipientPhone": "010-1234-5678",
                            "addressLine1": "서울특별시 강남구 테헤란로 123",
                            "addressLine2": "101호",
                            "zipCode": "06234"
                          }
                        }
                    """.trimIndent()
                }

                result.andExpect {
                    status { isCreated() }
                    jsonPath("$.rentalId") { value(1001) }
                    jsonPath("$.status") { value("REQUESTED") }
                    jsonPath("$.productId") { value(42) }
                }
            }
        }

        When("기간 충돌이 발생하면") {
            Then("409 RENTAL_PERIOD_CONFLICT가 반환된다") {
                every { requestRentalUseCase.execute(any()) } throws BusinessException(
                    errorCode = ErrorCode.RENTAL_PERIOD_CONFLICT,
                )

                val result = mockMvc.post("/api/v1/rentals") {
                    contentType = MediaType.APPLICATION_JSON
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "11")
                    content = """
                        {
                          "productId": 42,
                          "lenderId": 22,
                          "startDate": "2026-05-01T00:00:00+09:00",
                          "endDate": "2026-05-07T00:00:00+09:00",
                          "totalAmount": 70000,
                          "depositAmount": 50000,
                          "deliveryInfo": {
                            "recipientName": "홍길동",
                            "recipientPhone": "010-1234-5678",
                            "addressLine1": "서울",
                            "zipCode": "06234"
                          }
                        }
                    """.trimIndent()
                }

                result.andExpect {
                    status { isConflict() }
                    jsonPath("$.code") { value("RENTAL_PERIOD_CONFLICT") }
                }
            }
        }
    }

    Given("PATCH /api/v1/rentals/{id}/approve") {

        When("정상적인 승인 요청을 보내면") {
            Then("200 OK와 APPROVED 상태가 반환된다") {
                every { approveRentalUseCase.execute(any()) } returns rentalResponse(RentalStatus.APPROVED)

                val result = mockMvc.patch("/api/v1/rentals/1001/approve") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "22")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.status") { value("APPROVED") }
                }
            }
        }

        When("등록자가 아닌 사용자가 승인하면") {
            Then("403 RENTAL_LENDER_ONLY가 반환된다") {
                every { approveRentalUseCase.execute(any()) } throws BusinessException(
                    errorCode = ErrorCode.RENTAL_LENDER_ONLY,
                )

                val result = mockMvc.patch("/api/v1/rentals/1001/approve") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "99")
                }

                result.andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("RENTAL_LENDER_ONLY") }
                }
            }
        }
    }

    Given("PATCH /api/v1/rentals/{id}/reject") {

        When("정상적인 거절 요청을 보내면") {
            Then("200 OK와 CANCELLED 상태가 반환된다") {
                every { rejectRentalUseCase.execute(any()) } returns rentalResponse(RentalStatus.CANCELLED)

                val result = mockMvc.patch("/api/v1/rentals/1001/reject") {
                    contentType = MediaType.APPLICATION_JSON
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "22")
                    content = """{"reason": "해당 기간에 다른 일정이 생겼습니다."}"""
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.status") { value("CANCELLED") }
                }
            }
        }
    }

    Given("POST /api/v1/rentals/{id}/payment") {

        When("정상적인 결제 요청을 보내면") {
            Then("200 OK와 결제 정보가 반환된다") {
                every { processPaymentUseCase.execute(any()) } returns ProcessPaymentResult(
                    rentalId = 1001L,
                    paymentId = 5001L,
                    rentalStatus = RentalStatus.PAID,
                    paymentStatus = PaymentStatus.COMPLETED,
                    externalPaymentId = "5zJ4xY7m0kODnyRpQWGrN2eqXgarbMe7zEX5sGR0",
                    paidAt = now,
                )

                val result = mockMvc.post("/api/v1/rentals/1001/payment") {
                    contentType = MediaType.APPLICATION_JSON
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "11")
                    content = """
                        {
                          "paymentKey": "5zJ4xY7m0kODnyRpQWGrN2eqXgarbMe7zEX5sGR0",
                          "orderId": "RC-1001-1713063600000",
                          "amount": 120000,
                          "paymentMethod": "CARD"
                        }
                    """.trimIndent()
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.rentalId") { value(1001) }
                    jsonPath("$.rentalStatus") { value("PAID") }
                    jsonPath("$.paymentStatus") { value("COMPLETED") }
                }
            }
        }
    }

    Given("PATCH /api/v1/rentals/{id}/start") {

        When("정상적인 대여 시작 요청을 보내면") {
            Then("200 OK와 IN_USE 상태가 반환된다") {
                every { startRentalUseCase.execute(any()) } returns rentalResponse(RentalStatus.IN_USE)

                val result = mockMvc.patch("/api/v1/rentals/1001/start") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "22")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.status") { value("IN_USE") }
                }
            }
        }
    }

    Given("PATCH /api/v1/rentals/{id}/return") {

        When("정상적인 반납 요청을 보내면") {
            Then("200 OK와 RETURNED 상태가 반환된다") {
                every { returnRentalUseCase.execute(any()) } returns rentalResponse(RentalStatus.RETURNED)

                val result = mockMvc.patch("/api/v1/rentals/1001/return") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "11")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.status") { value("RETURNED") }
                }
            }
        }
    }

    Given("DELETE /api/v1/rentals/{id}") {

        When("정상적인 취소 요청을 보내면") {
            Then("200 OK와 CANCELLED 상태가 반환된다") {
                every { cancelRentalUseCase.execute(any()) } returns rentalResponse(RentalStatus.CANCELLED)

                val result = mockMvc.delete("/api/v1/rentals/1001") {
                    contentType = MediaType.APPLICATION_JSON
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "11")
                    content = """{"reason": "일정이 변경되었습니다."}"""
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.status") { value("CANCELLED") }
                }
            }
        }

        When("당사자가 아닌 사용자가 취소하면") {
            Then("403 RENTAL_ACCESS_DENIED가 반환된다") {
                every { cancelRentalUseCase.execute(any()) } throws BusinessException(
                    errorCode = ErrorCode.RENTAL_ACCESS_DENIED,
                )

                val result = mockMvc.delete("/api/v1/rentals/1001") {
                    contentType = MediaType.APPLICATION_JSON
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "99")
                    content = """{"reason": "사유"}"""
                }

                result.andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("RENTAL_ACCESS_DENIED") }
                }
            }
        }
    }

    Given("GET /api/v1/rentals") {

        When("내 대여 목록을 조회하면") {
            Then("200 OK와 목록이 반환된다") {
                every { getMyRentalsUseCase.execute(any()) } returns listOf(
                    RentalSummaryResponse(
                        rentalId = 1001L,
                        productId = 42L,
                        status = RentalStatus.IN_USE,
                        startDate = now.plusDays(1),
                        endDate = now.plusDays(7),
                        totalAmount = 120000L,
                        requestedAt = now,
                    )
                )

                val result = mockMvc.get("/api/v1/rentals") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "11")
                    param("role", "RENTER")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$[0].rentalId") { value(1001) }
                    jsonPath("$[0].status") { value("IN_USE") }
                }
            }
        }
    }

    Given("GET /api/v1/rentals/{id}") {

        When("대여 상세를 조회하면") {
            Then("200 OK와 상세 정보가 반환된다") {
                every { getRentalDetailUseCase.execute(1001L, 11L) } returns RentalDetailResponse(
                    rentalId = 1001L,
                    renterId = 11L,
                    lenderId = 22L,
                    productId = 42L,
                    status = RentalStatus.IN_USE,
                    startDate = now.plusDays(1),
                    endDate = now.plusDays(7),
                    totalAmount = 120000L,
                    depositAmount = 50000L,
                    orderId = "RC-42-1713063600000",
                    cancelReason = null,
                    requestedAt = now,
                    approvedAt = now.minusHours(2),
                    paidAt = now.minusHours(1),
                    startedAt = now,
                    returnedAt = null,
                    cancelledAt = null,
                    deliveryInfo = deliveryInfoResp,
                    payment = null,
                )

                val result = mockMvc.get("/api/v1/rentals/1001") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "11")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.rentalId") { value(1001) }
                    jsonPath("$.status") { value("IN_USE") }
                    jsonPath("$.renterId") { value(11) }
                }
            }
        }

        When("당사자가 아닌 사용자가 조회하면") {
            Then("403 RENTAL_ACCESS_DENIED가 반환된다") {
                every { getRentalDetailUseCase.execute(1001L, 99L) } throws BusinessException(
                    errorCode = ErrorCode.RENTAL_ACCESS_DENIED,
                )

                val result = mockMvc.get("/api/v1/rentals/1001") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "99")
                }

                result.andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("RENTAL_ACCESS_DENIED") }
                }
            }
        }
    }
})
