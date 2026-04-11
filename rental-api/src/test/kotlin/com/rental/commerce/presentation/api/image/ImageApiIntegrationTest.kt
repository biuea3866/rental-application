package com.rental.commerce.presentation.api.image

import com.rental.commerce.infrastructure.auth.JwtProvider
import io.kotest.core.spec.style.BehaviorSpec
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class ImageApiIntegrationTest(
    private val mockMvc: MockMvc,
    private val jwtProvider: JwtProvider,
) : BehaviorSpec({

    Given("POST /api/v1/images/presigned-url") {

        When("인증된 유저가 정상 요청을 보내면") {
            val accessToken = jwtProvider.createAccessToken(userId = 1L, role = "USER")

            val result = mockMvc.post("/api/v1/images/presigned-url") {
                header("Authorization", "Bearer $accessToken")
                contentType = MediaType.APPLICATION_JSON
                content = """{"fileName":"test.jpg","contentType":"image/jpeg","bucket":"products"}"""
            }

            Then("200 OK와 uploadUrl, objectKey가 반환된다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("$.uploadUrl") { isNotEmpty() }
                    jsonPath("$.objectKey") { isNotEmpty() }
                }
            }
        }

        When("인증 없이 요청하면") {
            val result = mockMvc.post("/api/v1/images/presigned-url") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"fileName":"test.jpg","contentType":"image/jpeg","bucket":"products"}"""
            }

            Then("401 Unauthorized가 반환된다") {
                result.andExpect {
                    status { isUnauthorized() }
                }
            }
        }

        When("지원하지 않는 contentType으로 요청하면") {
            val accessToken = jwtProvider.createAccessToken(userId = 1L, role = "USER")

            val result = mockMvc.post("/api/v1/images/presigned-url") {
                header("Authorization", "Bearer $accessToken")
                contentType = MediaType.APPLICATION_JSON
                content = """{"fileName":"readme.txt","contentType":"text/plain","bucket":"products"}"""
            }

            Then("400 INVALID_FILE_TYPE 에러가 반환된다") {
                result.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("INVALID_FILE_TYPE") }
                }
            }
        }
    }
})
