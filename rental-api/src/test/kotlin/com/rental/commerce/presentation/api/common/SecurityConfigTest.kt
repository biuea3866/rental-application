package com.rental.commerce.presentation.api.common

import com.rental.commerce.infrastructure.auth.JwtClaims
import com.rental.commerce.infrastructure.auth.JwtProvider
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.ZonedDateTime

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest(
    private val mockMvc: MockMvc,
    private val jwtProvider: JwtProvider,
) : BehaviorSpec({

    Given("인증이 필요 없는 경로") {

        When("GET /api/v1/auth/test 에 토큰 없이 요청하면") {
            val result = mockMvc.get("/api/v1/auth/test") {
                contentType = MediaType.APPLICATION_JSON
            }

            Then("200 OK가 반환된다") {
                result.andExpect {
                    status { isOk() }
                }
            }
        }

        When("GET /api/v1/price-guides/test 에 토큰 없이 요청하면") {
            val result = mockMvc.get("/api/v1/price-guides/test") {
                contentType = MediaType.APPLICATION_JSON
            }

            Then("200 OK가 반환된다") {
                result.andExpect {
                    status { isOk() }
                }
            }
        }
    }

    Given("인증이 필요한 경로") {

        When("GET /api/v1/protected 에 토큰 없이 요청하면") {
            val result = mockMvc.get("/api/v1/protected") {
                contentType = MediaType.APPLICATION_JSON
            }

            Then("401 Unauthorized가 반환된다") {
                result.andExpect {
                    status { isUnauthorized() }
                }
            }
        }

        When("GET /api/v1/protected 에 유효한 토큰으로 요청하면") {
            val token = jwtProvider.createAccessToken(userId = 1L, role = "USER")
            val result = mockMvc.get("/api/v1/protected") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
            }

            Then("200 OK가 반환된다") {
                result.andExpect {
                    status { isOk() }
                }
            }
        }

        When("GET /api/v1/protected 에 잘못된 토큰으로 요청하면") {
            val result = mockMvc.get("/api/v1/protected") {
                header("Authorization", "Bearer invalid.token.value")
                contentType = MediaType.APPLICATION_JSON
            }

            Then("401 Unauthorized가 반환된다") {
                result.andExpect {
                    status { isUnauthorized() }
                }
            }
        }

        When("GET /api/v1/protected 에 Bearer 접두사 없이 토큰을 전달하면") {
            val token = jwtProvider.createAccessToken(userId = 1L, role = "USER")
            val result = mockMvc.get("/api/v1/protected") {
                header("Authorization", token)
                contentType = MediaType.APPLICATION_JSON
            }

            Then("401 Unauthorized가 반환된다") {
                result.andExpect {
                    status { isUnauthorized() }
                }
            }
        }
    }

    Given("관리자 전용 경로") {

        When("GET /api/admin/dashboard 에 ADMIN 역할로 요청하면") {
            val token = jwtProvider.createAccessToken(userId = 1L, role = "ADMIN")
            val result = mockMvc.get("/api/admin/dashboard") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
            }

            Then("200 OK가 반환된다") {
                result.andExpect {
                    status { isOk() }
                }
            }
        }

        When("GET /api/admin/dashboard 에 일반 USER 역할로 요청하면") {
            val token = jwtProvider.createAccessToken(userId = 1L, role = "USER")
            val result = mockMvc.get("/api/admin/dashboard") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
            }

            Then("403 Forbidden이 반환된다") {
                result.andExpect {
                    status { isForbidden() }
                }
            }
        }

        When("GET /api/admin/dashboard 에 토큰 없이 요청하면") {
            val result = mockMvc.get("/api/admin/dashboard") {
                contentType = MediaType.APPLICATION_JSON
            }

            Then("401 Unauthorized가 반환된다") {
                result.andExpect {
                    status { isUnauthorized() }
                }
            }
        }
    }

    Given("CORS 설정") {

        When("다른 Origin에서 요청하면") {
            val result = mockMvc.get("/api/v1/auth/test") {
                header("Origin", "http://localhost:3000")
                contentType = MediaType.APPLICATION_JSON
            }

            Then("CORS 헤더가 포함된다") {
                result.andExpect {
                    status { isOk() }
                    header {
                        string("Access-Control-Allow-Origin", "http://localhost:3000")
                    }
                }
            }
        }
    }
}) {

    @TestConfiguration
    class TestSecurityControllers {

        @RestController
        class AuthTestController {
            @GetMapping("/api/v1/auth/test")
            fun authTest(): Map<String, String> = mapOf("status" to "ok")
        }

        @RestController
        class PriceGuideTestController {
            @GetMapping("/api/v1/price-guides/test")
            fun priceGuideTest(): Map<String, String> = mapOf("status" to "ok")
        }

        @RestController
        class ProtectedTestController {
            @GetMapping("/api/v1/protected")
            fun protectedEndpoint(): Map<String, String> = mapOf("status" to "ok")
        }

        @RestController
        class AdminTestController {
            @GetMapping("/api/admin/dashboard")
            fun adminDashboard(): Map<String, String> = mapOf("status" to "ok")
        }
    }
}
