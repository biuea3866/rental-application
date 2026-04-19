package com.rental.commerce.infrastructure.review

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.review.Review
import com.rental.commerce.infrastructure.common.config.JpaAuditingConfig
import com.rental.commerce.infrastructure.review.mysql.ReviewJpaRepository
import com.rental.commerce.infrastructure.review.mysql.ReviewRepositoryImpl
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    value = [
        JpaAuditingConfig::class,
        ReviewRepositoryImpl::class,
    ],
)
@ActiveProfiles("test")
class ReviewRepositoryImplTest(
    private val reviewJpaRepository: ReviewJpaRepository,
    private val reviewRepositoryImpl: ReviewRepositoryImpl,
) : BehaviorSpec({

    extensions(SpringExtension)

    fun createAndSave(
        renterId: Long = 1L,
        rentalId: Long = 10L,
        productId: Long = 100L,
        rating: Int = 5,
        content: String = "상태도 좋고 설명과 동일한 상품이었습니다.",
    ): Review {
        val review = Review.create(
            renterId = renterId,
            rentalId = rentalId,
            productId = productId,
            rating = rating,
            content = content,
        )
        return reviewJpaRepository.save(review)
    }

    // ─────────────────────────────────────────────────────────────
    // save — 정상 저장
    // ─────────────────────────────────────────────────────────────

    Given("save() — 정상 저장") {

        When("유효한 리뷰를 저장하면") {
            val saved = reviewRepositoryImpl.save(
                Review.create(
                    renterId = 1L,
                    rentalId = 1000L,
                    productId = 100L,
                    rating = 4,
                    content = "전반적으로 만족스러운 대여 경험이었습니다.",
                )
            )

            Then("id가 자동 생성된다") {
                (saved.id > 0L) shouldBe true
            }

            Then("renterId가 저장된다") {
                saved.renterId shouldBe 1L
            }

            Then("rentalId가 저장된다") {
                saved.rentalId shouldBe 1000L
            }

            Then("rating이 저장된다") {
                saved.rating shouldBe 4
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // save — rental_id UNIQUE 제약 위반
    // ─────────────────────────────────────────────────────────────

    Given("save() — rental_id UNIQUE 제약 위반") {

        When("동일한 rentalId로 리뷰를 두 번 저장하면") {
            createAndSave(rentalId = 2000L)

            Then("DataIntegrityViolationException이 발생한다") {
                shouldThrow<DataIntegrityViolationException> {
                    createAndSave(rentalId = 2000L)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // findByRentalId — 조회
    // ─────────────────────────────────────────────────────────────

    Given("findByRentalId() — 조회") {

        When("존재하는 rentalId로 조회하면") {
            createAndSave(rentalId = 3000L)
            val result = reviewRepositoryImpl.findByRentalId(3000L)

            Then("리뷰가 반환된다") {
                result.shouldNotBeNull()
                result.rentalId shouldBe 3000L
            }
        }

        When("존재하지 않는 rentalId로 조회하면") {
            val result = reviewRepositoryImpl.findByRentalId(999999L)

            Then("null이 반환된다") {
                result.shouldBeNull()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // findByProductId — 페이지네이션 조회
    // ─────────────────────────────────────────────────────────────

    Given("findByProductId() — 페이지네이션 조회") {

        When("productId=500에 리뷰가 3개 있으면") {
            createAndSave(renterId = 1L, rentalId = 4001L, productId = 500L)
            createAndSave(renterId = 2L, rentalId = 4002L, productId = 500L)
            createAndSave(renterId = 3L, rentalId = 4003L, productId = 500L)

            val result = reviewRepositoryImpl.findByProductId(500L, PageQuery(page = 0, size = 20))

            Then("3개가 반환된다") {
                result.content.size shouldBe 3
                result.totalElements shouldBe 3L
            }
        }

        When("size=2, page=0으로 조회하면") {
            createAndSave(renterId = 4L, rentalId = 5001L, productId = 600L)
            createAndSave(renterId = 5L, rentalId = 5002L, productId = 600L)
            createAndSave(renterId = 6L, rentalId = 5003L, productId = 600L)

            val result = reviewRepositoryImpl.findByProductId(600L, PageQuery(page = 0, size = 2))

            Then("2개만 반환되고 totalElements는 3이다") {
                result.content.size shouldBe 2
                result.totalElements shouldBe 3L
                result.totalPages shouldBe 2
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // existsByRentalId — 존재 여부
    // ─────────────────────────────────────────────────────────────

    Given("existsByRentalId() — 존재 여부 확인") {

        When("해당 rentalId의 리뷰가 존재하면") {
            createAndSave(rentalId = 6000L)

            Then("true를 반환한다") {
                reviewRepositoryImpl.existsByRentalId(6000L) shouldBe true
            }
        }

        When("해당 rentalId의 리뷰가 없으면") {
            Then("false를 반환한다") {
                reviewRepositoryImpl.existsByRentalId(999998L) shouldBe false
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

        init {
            mysqlContainer.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysqlContainer.jdbcUrl }
            registry.add("spring.datasource.username") { mysqlContainer.username }
            registry.add("spring.datasource.password") { mysqlContainer.password }
            registry.add("spring.datasource.driver-class-name") { mysqlContainer.driverClassName }
        }
    }
}
