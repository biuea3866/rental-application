package com.rental.commerce.presentation.api.auth

import io.kotest.core.spec.style.BehaviorSpec

class AuthApiIntegrationTest : BehaviorSpec({

    Given("POST /api/v1/auth/refresh") {

        When("유효한 Refresh Token으로 갱신 요청하면") {
            Then("새 Access + Refresh Token이 반환된다 (200)") {
                // TODO: 전체 스프링 컨텍스트 + TestContainers 구현 후 활성화
                // mockMvc.post("/api/v1/auth/refresh") {
                //     contentType = MediaType.APPLICATION_JSON
                //     content = """{"refreshToken": "$validRefreshToken"}"""
                // }.andExpect {
                //     status { isOk() }
                //     jsonPath("$.accessToken") { isNotEmpty() }
                //     jsonPath("$.refreshToken") { isNotEmpty() }
                // }
            }
        }

        When("만료된 Refresh Token으로 갱신 요청하면") {
            Then("401 Unauthorized가 반환된다") {
                // TODO
            }
        }

        When("이미 사용된 Refresh Token을 재사용하면") {
            Then("해당 Token Family 전체 폐기 + 401 반환") {
                // TODO: Family Detection 통합 테스트
            }
        }
    }

    Given("인증이 필요한 API") {

        When("Authorization 헤더 없이 요청하면") {
            Then("401 Unauthorized가 반환된다") {
                // TODO
            }
        }

        When("유효한 Access Token으로 요청하면") {
            Then("정상 응답이 반환된다") {
                // TODO
            }
        }

        When("만료된 Access Token으로 요청하면") {
            Then("401 Unauthorized가 반환된다") {
                // TODO
            }
        }
    }
})
