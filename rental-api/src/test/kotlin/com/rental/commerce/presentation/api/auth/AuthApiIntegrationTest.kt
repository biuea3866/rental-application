package com.rental.commerce.presentation.api.auth

import com.rental.commerce.domain.common.PasswordHasher
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserRepository
import com.rental.commerce.domain.user.UserRole
import com.rental.commerce.infrastructure.auth.JwtProvider
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIntegrationTest(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtProvider: JwtProvider,
) : BehaviorSpec({

    extensions(SpringExtension)

    Given("POST /api/v1/auth/login") {

        When("유효한 이메일과 비밀번호로 로그인하면") {
            val hashedPassword = passwordHasher.hash("password123!")
            val user = User(
                email = "login-success@example.com",
                name = "로그인테스트",
                phone = "01011111111",
                passwordHash = hashedPassword,
                role = UserRole.RENTER,
            )
            userRepository.save(user)

            val result = mockMvc.post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":"login-success@example.com","password":"password123!"}"""
            }

            Then("200 OK와 토큰이 반환된다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("$.accessToken") { isNotEmpty() }
                    jsonPath("$.refreshToken") { isNotEmpty() }
                    jsonPath("$.tokenFamily") { isNotEmpty() }
                    jsonPath("$.userId") { isNumber() }
                }
            }
        }

        When("잘못된 비밀번호로 로그인하면") {
            val hashedPassword = passwordHasher.hash("correct-password!")
            val user = User(
                email = "login-wrong-pw@example.com",
                name = "비밀번호오류",
                phone = "01022222222",
                passwordHash = hashedPassword,
                role = UserRole.RENTER,
            )
            userRepository.save(user)

            val result = mockMvc.post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":"login-wrong-pw@example.com","password":"wrong-password!"}"""
            }

            Then("401 INVALID_PASSWORD 에러가 반환된다") {
                result.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.code") { value("INVALID_PASSWORD") }
                }
            }
        }
    }

    Given("POST /api/v1/auth/refresh") {

        When("로그인 후 발급받은 Refresh Token으로 갱신하면") {
            val hashedPassword = passwordHasher.hash("password123!")
            val user = User(
                email = "refresh-test@example.com",
                name = "리프레시테스트",
                phone = "01033333333",
                passwordHash = hashedPassword,
                role = UserRole.RENTER,
            )
            userRepository.save(user)

            // 먼저 로그인하여 유효한 refreshToken과 tokenFamily를 획득
            val loginResult = mockMvc.post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":"refresh-test@example.com","password":"password123!"}"""
            }.andReturn()

            val responseBody = loginResult.response.contentAsString
            val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
            val loginResponse = objectMapper.readTree(responseBody)
            val refreshToken = loginResponse.get("refreshToken").asText()
            val tokenFamily = loginResponse.get("tokenFamily").asText()

            val result = mockMvc.post("/api/v1/auth/refresh") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"refreshToken":"$refreshToken","tokenFamily":"$tokenFamily"}"""
            }

            Then("새 Access + Refresh Token이 반환된다 (200)") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("$.accessToken") { isNotEmpty() }
                    jsonPath("$.refreshToken") { isNotEmpty() }
                    jsonPath("$.tokenFamily") { isNotEmpty() }
                    jsonPath("$.userId") { isNumber() }
                }
            }
        }

        When("유효하지 않은 Refresh Token으로 갱신하면") {
            val result = mockMvc.post("/api/v1/auth/refresh") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"refreshToken":"invalid-token-value","tokenFamily":"non-existent-family"}"""
            }

            Then("401 Unauthorized가 반환된다") {
                result.andExpect {
                    status { isUnauthorized() }
                }
            }
        }
    }
}) {
    companion object {
        private val mysqlContainer = MySQLContainer("mysql:8.0").apply {
            withDatabaseName("rental_commerce_test")
            withUsername("test")
            withPassword("test")
        }

        private val redisContainer = GenericContainer("redis:7-alpine").apply {
            withExposedPorts(6379)
        }

        init {
            mysqlContainer.start()
            redisContainer.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysqlContainer.jdbcUrl }
            registry.add("spring.datasource.username") { mysqlContainer.username }
            registry.add("spring.datasource.password") { mysqlContainer.password }
            registry.add("spring.datasource.driver-class-name") { mysqlContainer.driverClassName }
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("minio.endpoint") { "http://localhost:9000" }
            registry.add("minio.access-key") { "minioadmin" }
            registry.add("minio.secret-key") { "minioadmin" }
        }
    }
}
