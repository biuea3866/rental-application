package com.rental.commerce.presentation.api.priceguide

import com.rental.commerce.application.priceguide.GetPriceGuideUseCase
import com.rental.commerce.application.priceguide.PriceGuideResponse
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class PriceGuideApiControllerTest : BehaviorSpec({

    val getPriceGuideUseCase = mockk<GetPriceGuideUseCase>()
    val controller = PriceGuideApiController(getPriceGuideUseCase)
    val mockMvc = MockMvcBuilders.standaloneSetup(controller).build()

    Given("GET /api/v1/price-guides") {

        When("유효한 categoryCode로 요청하면") {
            val categoryCode = "ELECTRONICS"
            val responses = listOf(
                PriceGuideResponse(
                    categoryCode = "ELECTRONICS",
                    rentalUnit = "DAILY",
                    minPrice = 5000L,
                    maxPrice = 50000L,
                ),
                PriceGuideResponse(
                    categoryCode = "ELECTRONICS",
                    rentalUnit = "MONTHLY",
                    minPrice = 50000L,
                    maxPrice = 500000L,
                ),
                PriceGuideResponse(
                    categoryCode = "ELECTRONICS",
                    rentalUnit = "YEARLY",
                    minPrice = 500000L,
                    maxPrice = 5000000L,
                ),
            )

            every { getPriceGuideUseCase.execute(categoryCode) } returns responses

            val result = mockMvc.get("/api/v1/price-guides") {
                param("categoryCode", categoryCode)
                accept = MediaType.APPLICATION_JSON
            }

            Then("200 OK가 반환된다") {
                result.andExpect {
                    status { isOk() }
                }
            }

            Then("JSON 배열에 3개의 가이드 가격이 포함된다") {
                result.andExpect {
                    jsonPath("$.length()") { value(3) }
                }
            }

            Then("첫 번째 항목의 categoryCode가 올바르다") {
                result.andExpect {
                    jsonPath("$[0].categoryCode") { value("ELECTRONICS") }
                }
            }

            Then("DAILY 항목의 가격 범위가 올바르다") {
                result.andExpect {
                    jsonPath("$[0].rentalUnit") { value("DAILY") }
                    jsonPath("$[0].minPrice") { value(5000) }
                    jsonPath("$[0].maxPrice") { value(50000) }
                }
            }
        }

        When("존재하지 않는 categoryCode로 요청하면") {
            val categoryCode = "UNKNOWN"

            every { getPriceGuideUseCase.execute(categoryCode) } returns emptyList()

            val result = mockMvc.get("/api/v1/price-guides") {
                param("categoryCode", categoryCode)
                accept = MediaType.APPLICATION_JSON
            }

            Then("200 OK와 빈 배열이 반환된다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("$.length()") { value(0) }
                }
            }
        }

        When("categoryCode 파라미터 없이 요청하면") {
            val result = mockMvc.get("/api/v1/price-guides") {
                accept = MediaType.APPLICATION_JSON
            }

            Then("400 Bad Request가 반환된다") {
                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }
    }
})
