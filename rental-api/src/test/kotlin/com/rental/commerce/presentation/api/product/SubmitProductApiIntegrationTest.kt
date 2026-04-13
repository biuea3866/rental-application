package com.rental.commerce.presentation.api.product

import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductCondition
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.product.ProductStatus
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserRepository
import com.rental.commerce.domain.user.UserRole
import com.rental.commerce.infrastructure.auth.JwtProvider
import com.rental.commerce.infrastructure.product.ProductJpaRepository
import com.rental.commerce.infrastructure.user.UserJpaRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
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
class SubmitProductApiIntegrationTest(
    private val mockMvc: MockMvc,
    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val userJpaRepository: UserJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
) : BehaviorSpec({

    extensions(SpringExtension)

    beforeEach {
        productJpaRepository.deleteAllInBatch()
        userJpaRepository.deleteAllInBatch()
    }

    Given("POST /api/v1/products/drafts/{productId}/submit") {

        When("모든 필수 필드가 입력된 DRAFT 상품을 제출하면") {
            Then("200 OK와 UNDER_REVIEW 상태의 상품이 반환되고 DB에도 반영된다") {
                val user = userRepository.save(
                    User(
                        email = "test@example.com",
                        name = "테스트유저",
                        phone = "010-1234-5678",
                        passwordHash = "hashed",
                        role = UserRole.LENDER,
                    )
                )
                val userId = requireNotNull(user.id) { "저장된 User의 ID가 null입니다" }

                val product = productRepository.save(
                    Product(
                        userId = userId,
                        name = "맥북 프로 16인치",
                        description = "2024년형 M3 Max 맥북 프로",
                        categoryCode = "ELECTRONICS",
                        condition = ProductCondition.LIKE_NEW,
                        depositAmount = 500000L,
                        status = ProductStatus.DRAFT,
                        currentDraftStep = 5,
                    )
                )
                val productId = product.productId

                val accessToken = jwtProvider.createAccessToken(userId = userId, role = "USER")

                val result = mockMvc.post("/api/v1/products/drafts/$productId/submit") {
                    header("Authorization", "Bearer $accessToken")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.productId") { value(productId.toInt()) }
                    jsonPath("$.status") { value("UNDER_REVIEW") }
                    jsonPath("$.name") { value("맥북 프로 16인치") }
                    jsonPath("$.description") { value("2024년형 M3 Max 맥북 프로") }
                    jsonPath("$.categoryCode") { value("ELECTRONICS") }
                    jsonPath("$.condition") { value("LIKE_NEW") }
                    jsonPath("$.depositAmount") { value(500000) }
                }

                val savedProduct = productRepository.findById(productId)
                requireNotNull(savedProduct) { "제출된 상품이 DB에서 조회되지 않습니다" }
                savedProduct.status shouldBe ProductStatus.UNDER_REVIEW
            }
        }

        When("존재하지 않는 상품을 제출하면") {
            Then("404 PRODUCT_NOT_FOUND 에러가 반환된다") {
                val user = userRepository.save(
                    User(
                        email = "test2@example.com",
                        name = "테스트유저",
                        phone = "010-1234-5678",
                        passwordHash = "hashed",
                        role = UserRole.LENDER,
                    )
                )
                val userId = requireNotNull(user.id) { "저장된 User의 ID가 null입니다" }
                val accessToken = jwtProvider.createAccessToken(userId = userId, role = "USER")

                val result = mockMvc.post("/api/v1/products/drafts/99999/submit") {
                    header("Authorization", "Bearer $accessToken")
                }

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("PRODUCT_NOT_FOUND") }
                }
            }
        }

        When("소유자가 아닌 사용자가 상품을 제출하면") {
            Then("403 PRODUCT_OWNERSHIP_DENIED 에러가 반환된다") {
                val owner = userRepository.save(
                    User(
                        email = "owner@example.com",
                        name = "소유자",
                        phone = "010-1111-1111",
                        passwordHash = "hashed",
                        role = UserRole.LENDER,
                    )
                )
                val ownerId = requireNotNull(owner.id) { "저장된 Owner의 ID가 null입니다" }

                val otherUser = userRepository.save(
                    User(
                        email = "other@example.com",
                        name = "다른유저",
                        phone = "010-2222-2222",
                        passwordHash = "hashed",
                        role = UserRole.LENDER,
                    )
                )
                val otherUserId = requireNotNull(otherUser.id) { "저장된 Other User의 ID가 null입니다" }

                val product = productRepository.save(
                    Product(
                        userId = ownerId,
                        name = "맥북 프로",
                        description = "설명",
                        categoryCode = "ELECTRONICS",
                        condition = ProductCondition.LIKE_NEW,
                        depositAmount = 500000L,
                        status = ProductStatus.DRAFT,
                    )
                )

                val accessToken = jwtProvider.createAccessToken(userId = otherUserId, role = "USER")

                val result = mockMvc.post("/api/v1/products/drafts/${product.productId}/submit") {
                    header("Authorization", "Bearer $accessToken")
                }

                result.andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("PRODUCT_OWNERSHIP_DENIED") }
                }
            }
        }

        When("DRAFT가 아닌 상태의 상품을 제출하면") {
            Then("400 INVALID_STATE_TRANSITION 에러가 반환된다") {
                val user = userRepository.save(
                    User(
                        email = "test3@example.com",
                        name = "테스트유저",
                        phone = "010-1234-5678",
                        passwordHash = "hashed",
                        role = UserRole.LENDER,
                    )
                )
                val userId = requireNotNull(user.id) { "저장된 User의 ID가 null입니다" }

                val product = productRepository.save(
                    Product(
                        userId = userId,
                        name = "맥북 프로",
                        description = "설명",
                        categoryCode = "ELECTRONICS",
                        condition = ProductCondition.LIKE_NEW,
                        depositAmount = 500000L,
                        status = ProductStatus.UNDER_REVIEW,
                    )
                )

                val accessToken = jwtProvider.createAccessToken(userId = userId, role = "USER")

                val result = mockMvc.post("/api/v1/products/drafts/${product.productId}/submit") {
                    header("Authorization", "Bearer $accessToken")
                }

                result.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("INVALID_STATE_TRANSITION") }
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
        }
    }
}
