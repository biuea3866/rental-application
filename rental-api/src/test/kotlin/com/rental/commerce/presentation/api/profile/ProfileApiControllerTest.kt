package com.rental.commerce.presentation.api.profile

import com.fasterxml.jackson.databind.ObjectMapper
import com.rental.commerce.application.user.AddLenderProfileCommand
import com.rental.commerce.application.user.AddLenderProfileUseCase
import com.rental.commerce.application.user.AddRenterProfileCommand
import com.rental.commerce.application.user.AddRenterProfileUseCase
import com.rental.commerce.application.user.GetLenderProfileUseCase
import com.rental.commerce.application.user.GetRenterProfileUseCase
import com.rental.commerce.application.user.LenderProfileResponse
import com.rental.commerce.application.user.RenterProfileResponse
import com.rental.commerce.presentation.api.common.AuthenticatedRequestWrapper
import com.rental.commerce.presentation.api.common.MemberIdArgumentResolver
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ProfileApiControllerTest : BehaviorSpec({

    val addLenderProfileUseCase = mockk<AddLenderProfileUseCase>()
    val addRenterProfileUseCase = mockk<AddRenterProfileUseCase>()
    val getLenderProfileUseCase = mockk<GetLenderProfileUseCase>()
    val getRenterProfileUseCase = mockk<GetRenterProfileUseCase>()
    val controller = ProfileApiController(
        addLenderProfileUseCase,
        addRenterProfileUseCase,
        getLenderProfileUseCase,
        getRenterProfileUseCase,
    )
    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setCustomArgumentResolvers(MemberIdArgumentResolver())
        .build()
    val objectMapper = ObjectMapper()

    Given("POST /api/v1/users/me/lender-profile") {

        When("유효한 요청으로 대여자 프로필을 생성하면") {
            val request = CreateLenderProfileRequest(lenderType = "INDIVIDUAL")
            val response = LenderProfileResponse(
                lenderProfileId = 1L,
                userId = 1L,
                lenderType = "INDIVIDUAL",
                verificationStatus = "PENDING",
                settlementAccountBank = null,
                settlementAccountNumber = null,
            )
            every { addLenderProfileUseCase.execute(any<AddLenderProfileCommand>()) } returns response

            Then("201 Created가 반환된다") {
                mockMvc.post("/api/v1/users/me/lender-profile") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }.andExpect {
                    status { isCreated() }
                }
            }

            Then("응답에 프로필 정보가 포함된다") {
                mockMvc.post("/api/v1/users/me/lender-profile") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }.andExpect {
                    jsonPath("$.userId") { value(1) }
                    jsonPath("$.lenderType") { value("INDIVIDUAL") }
                    jsonPath("$.verificationStatus") { value("PENDING") }
                }
            }
        }
    }

    Given("POST /api/v1/users/me/renter-profile") {

        When("유효한 요청으로 임차인 프로필을 생성하면") {
            val response = RenterProfileResponse(
                renterProfileId = 1L,
                userId = 1L,
                trustGrade = "BRONZE",
                totalTransactionCount = 0,
                shippingAddress = null,
            )
            every { addRenterProfileUseCase.execute(any<AddRenterProfileCommand>()) } returns response

            Then("201 Created가 반환된다") {
                mockMvc.post("/api/v1/users/me/renter-profile") {
                    contentType = MediaType.APPLICATION_JSON
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }.andExpect {
                    status { isCreated() }
                }
            }

            Then("응답에 프로필 정보가 포함된다") {
                mockMvc.post("/api/v1/users/me/renter-profile") {
                    contentType = MediaType.APPLICATION_JSON
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }.andExpect {
                    jsonPath("$.userId") { value(1) }
                    jsonPath("$.trustGrade") { value("BRONZE") }
                    jsonPath("$.totalTransactionCount") { value(0) }
                }
            }
        }
    }

    Given("GET /api/v1/users/me/lender-profile") {

        When("대여자 프로필을 조회하면") {
            val response = LenderProfileResponse(
                lenderProfileId = 1L,
                userId = 1L,
                lenderType = "INDIVIDUAL",
                verificationStatus = "VERIFIED",
                settlementAccountBank = "신한은행",
                settlementAccountNumber = "110-123-456789",
            )
            every { getLenderProfileUseCase.execute(1L) } returns response

            Then("200 OK가 반환된다") {
                mockMvc.get("/api/v1/users/me/lender-profile") {
                    accept = MediaType.APPLICATION_JSON
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }.andExpect {
                    status { isOk() }
                }
            }

            Then("응답에 프로필 정보가 포함된다") {
                mockMvc.get("/api/v1/users/me/lender-profile") {
                    accept = MediaType.APPLICATION_JSON
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }.andExpect {
                    jsonPath("$.userId") { value(1) }
                    jsonPath("$.lenderType") { value("INDIVIDUAL") }
                    jsonPath("$.verificationStatus") { value("VERIFIED") }
                    jsonPath("$.settlementAccountBank") { value("신한은행") }
                }
            }
        }
    }

    Given("GET /api/v1/users/me/renter-profile") {

        When("임차인 프로필을 조회하면") {
            val response = RenterProfileResponse(
                renterProfileId = 1L,
                userId = 1L,
                trustGrade = "SILVER",
                totalTransactionCount = 15,
                shippingAddress = "서울시 강남구",
            )
            every { getRenterProfileUseCase.execute(1L) } returns response

            Then("200 OK가 반환된다") {
                mockMvc.get("/api/v1/users/me/renter-profile") {
                    accept = MediaType.APPLICATION_JSON
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }.andExpect {
                    status { isOk() }
                }
            }

            Then("응답에 프로필 정보가 포함된다") {
                mockMvc.get("/api/v1/users/me/renter-profile") {
                    accept = MediaType.APPLICATION_JSON
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "1")
                }.andExpect {
                    jsonPath("$.userId") { value(1) }
                    jsonPath("$.trustGrade") { value("SILVER") }
                    jsonPath("$.totalTransactionCount") { value(15) }
                }
            }
        }
    }
})
