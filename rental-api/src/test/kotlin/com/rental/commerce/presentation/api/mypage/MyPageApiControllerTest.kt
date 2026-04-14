package com.rental.commerce.presentation.api.mypage

import com.fasterxml.jackson.databind.ObjectMapper
import com.rental.commerce.application.user.GetMyPageUseCase
import com.rental.commerce.application.user.LenderProfileResponse
import com.rental.commerce.application.user.MyPageResponse
import com.rental.commerce.application.user.RenterProfileResponse
import com.rental.commerce.application.user.UpdateLenderProfileCommand
import com.rental.commerce.application.user.UpdateLenderProfileUseCase
import com.rental.commerce.application.user.UpdateRenterProfileCommand
import com.rental.commerce.application.user.UpdateRenterProfileUseCase
import com.rental.commerce.application.user.UpdateUserProfileCommand
import com.rental.commerce.application.user.UpdateUserProfileUseCase
import com.rental.commerce.presentation.api.common.AuthenticatedRequestWrapper
import com.rental.commerce.presentation.api.common.MemberIdArgumentResolver
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class MyPageApiControllerTest : BehaviorSpec({

    val getMyPageUseCase = mockk<GetMyPageUseCase>()
    val updateUserProfileUseCase = mockk<UpdateUserProfileUseCase>()
    val updateLenderProfileUseCase = mockk<UpdateLenderProfileUseCase>()
    val updateRenterProfileUseCase = mockk<UpdateRenterProfileUseCase>()
    val controller = MyPageApiController(
        getMyPageUseCase = getMyPageUseCase,
        updateUserProfileUseCase = updateUserProfileUseCase,
        updateLenderProfileUseCase = updateLenderProfileUseCase,
        updateRenterProfileUseCase = updateRenterProfileUseCase,
    )
    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setCustomArgumentResolvers(MemberIdArgumentResolver())
        .build()
    val objectMapper = ObjectMapper()

    Given("GET /api/v1/mypage") {

        When("인증된 사용자가 마이페이지를 조회하면") {
            val response = MyPageResponse(
                userId = 1L,
                email = "test@example.com",
                name = "홍길동",
                phone = "010-1234-5678",
                role = "BOTH",
                lenderProfile = LenderProfileResponse(
                    lenderProfileId = 1L,
                    userId = 1L,
                    lenderType = "INDIVIDUAL",
                    verificationStatus = "VERIFIED",
                    settlementAccountBank = "신한은행",
                    settlementAccountNumber = "110-123-456789",
                ),
                renterProfile = RenterProfileResponse(
                    renterProfileId = 1L,
                    userId = 1L,
                    trustGrade = "SILVER",
                    totalTransactionCount = 15,
                    shippingAddress = "서울시 강남구",
                ),
            )

            every { getMyPageUseCase.execute(1L) } returns response

            val result = mockMvc.get("/api/v1/mypage") {
                accept = MediaType.APPLICATION_JSON
                header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
            }

            Then("200 OK가 반환된다") {
                result.andExpect {
                    status { isOk() }
                }
            }

            Then("사용자 기본 정보가 포함된다") {
                result.andExpect {
                    jsonPath("$.userId") { value(1) }
                    jsonPath("$.email") { value("test@example.com") }
                    jsonPath("$.name") { value("홍길동") }
                    jsonPath("$.phone") { value("010-1234-5678") }
                    jsonPath("$.role") { value("BOTH") }
                }
            }

            Then("대여자 프로필이 포함된다") {
                result.andExpect {
                    jsonPath("$.lenderProfile.lenderType") { value("INDIVIDUAL") }
                    jsonPath("$.lenderProfile.verificationStatus") { value("VERIFIED") }
                }
            }

            Then("임차인 프로필이 포함된다") {
                result.andExpect {
                    jsonPath("$.renterProfile.trustGrade") { value("SILVER") }
                    jsonPath("$.renterProfile.totalTransactionCount") { value(15) }
                }
            }
        }
    }

    Given("PATCH /api/v1/mypage/profile") {

        When("이름과 전화번호를 수정하면") {
            val request = UpdateUserProfileRequest(
                name = "김철수",
                phone = "010-9999-8888",
            )

            every { updateUserProfileUseCase.execute(any<UpdateUserProfileCommand>()) } just runs

            val result = mockMvc.patch("/api/v1/mypage/profile") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
            }

            Then("204 No Content가 반환된다") {
                result.andExpect {
                    status { isNoContent() }
                }
            }

            Then("UseCase가 호출된다") {
                verify { updateUserProfileUseCase.execute(any<UpdateUserProfileCommand>()) }
            }
        }
    }

    Given("PATCH /api/v1/mypage/lender-profile") {

        When("정산 계좌 정보를 수정하면") {
            val request = UpdateLenderProfileRequest(
                settlementAccountBank = "국민은행",
                settlementAccountNumber = "999-888-777666",
            )
            val response = LenderProfileResponse(
                lenderProfileId = 1L,
                userId = 1L,
                lenderType = "INDIVIDUAL",
                verificationStatus = "VERIFIED",
                settlementAccountBank = "국민은행",
                settlementAccountNumber = "999-888-777666",
            )

            every { updateLenderProfileUseCase.execute(any<UpdateLenderProfileCommand>()) } returns response

            val result = mockMvc.patch("/api/v1/mypage/lender-profile") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
            }

            Then("200 OK가 반환된다") {
                result.andExpect {
                    status { isOk() }
                }
            }

            Then("수정된 프로필 정보가 반환된다") {
                result.andExpect {
                    jsonPath("$.settlementAccountBank") { value("국민은행") }
                    jsonPath("$.settlementAccountNumber") { value("999-888-777666") }
                }
            }
        }
    }

    Given("PATCH /api/v1/mypage/renter-profile") {

        When("배송지 주소를 수정하면") {
            val request = UpdateRenterProfileRequest(
                shippingAddress = "서울시 서초구",
            )
            val response = RenterProfileResponse(
                renterProfileId = 1L,
                userId = 1L,
                trustGrade = "SILVER",
                totalTransactionCount = 15,
                shippingAddress = "서울시 서초구",
            )

            every { updateRenterProfileUseCase.execute(any<UpdateRenterProfileCommand>()) } returns response

            val result = mockMvc.patch("/api/v1/mypage/renter-profile") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
            }

            Then("200 OK가 반환된다") {
                result.andExpect {
                    status { isOk() }
                }
            }

            Then("수정된 프로필 정보가 반환된다") {
                result.andExpect {
                    jsonPath("$.shippingAddress") { value("서울시 서초구") }
                }
            }
        }
    }
})
