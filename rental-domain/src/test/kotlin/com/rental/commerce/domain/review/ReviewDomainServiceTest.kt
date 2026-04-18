package com.rental.commerce.domain.review

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.common.ReviewAlreadyExistsException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ReviewDomainServiceTest : BehaviorSpec({

    fun newMocks(): Pair<ReviewRepository, ReviewDomainService> {
        val repo = mockk<ReviewRepository>()
        return repo to ReviewDomainService(repo)
    }

    fun validContent() = "상태도 좋고 설명과 동일한 상품이었습니다."

    fun createSavedReview(
        renterId: Long = 1L,
        rentalId: Long = 10L,
        productId: Long = 100L,
        rating: Int = 5,
        content: String = validContent(),
    ) = Review.create(renterId, rentalId, productId, rating, content)

    // ─────────────────────────────────────────────────────────────
    // createReview — 정상 생성
    // ─────────────────────────────────────────────────────────────

    Given("createReview() — 정상 생성") {

        When("중복 리뷰가 없고 유효한 rating과 content를 전달하면") {
            val (repo, service) = newMocks()

            every { repo.existsByRentalId(10L) } returns false
            every { repo.save(any()) } answers { firstArg() }

            val review = service.createReview(
                renterId = 1L,
                rentalId = 10L,
                productId = 100L,
                rating = 5,
                content = validContent(),
            )

            Then("Review가 저장된다") {
                verify(exactly = 1) { repo.save(any()) }
            }

            Then("반환된 Review의 rentalId가 일치한다") {
                review.rentalId shouldBe 10L
            }

            Then("반환된 Review의 rating이 일치한다") {
                review.rating shouldBe 5
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // createReview — 중복 리뷰 예외
    // ─────────────────────────────────────────────────────────────

    Given("createReview() — 중복 리뷰 예외") {

        When("동일 rentalId로 이미 리뷰가 존재하면") {
            val (repo, service) = newMocks()

            every { repo.existsByRentalId(10L) } returns true

            Then("ReviewAlreadyExistsException이 발생한다") {
                shouldThrow<ReviewAlreadyExistsException> {
                    service.createReview(
                        renterId = 1L,
                        rentalId = 10L,
                        productId = 100L,
                        rating = 5,
                        content = validContent(),
                    )
                }
            }

            Then("save()는 호출되지 않는다") {
                verify(exactly = 0) { repo.save(any()) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // createReview — 잘못된 rating
    // ─────────────────────────────────────────────────────────────

    Given("createReview() — 잘못된 rating") {

        When("rating이 0이면") {
            val (repo, service) = newMocks()

            every { repo.existsByRentalId(10L) } returns false

            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    service.createReview(
                        renterId = 1L,
                        rentalId = 10L,
                        productId = 100L,
                        rating = 0,
                        content = validContent(),
                    )
                }
            }

            Then("save()는 호출되지 않는다") {
                verify(exactly = 0) { repo.save(any()) }
            }
        }

        When("rating이 6이면") {
            val (repo, service) = newMocks()

            every { repo.existsByRentalId(10L) } returns false

            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    service.createReview(
                        renterId = 1L,
                        rentalId = 10L,
                        productId = 100L,
                        rating = 6,
                        content = validContent(),
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getProductReviews
    // ─────────────────────────────────────────────────────────────

    Given("getProductReviews() — productId 기준 조회") {

        When("productId=100에 리뷰가 3개 있으면") {
            val (repo, service) = newMocks()
            val reviews = listOf(
                createSavedReview(rentalId = 10L, rating = 5),
                createSavedReview(rentalId = 11L, rating = 4),
                createSavedReview(rentalId = 12L, rating = 3),
            )
            val pageQuery = PageQuery(page = 0, size = 10)

            every { repo.findByProductId(100L, pageQuery) } returns PageResult(
                content = reviews,
                totalElements = 3L,
                totalPages = 1,
            )

            val result = service.getProductReviews(100L, pageQuery)

            Then("3개의 리뷰가 반환된다") {
                result.content.size shouldBe 3
                result.totalElements shouldBe 3L
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getMyReviews
    // ─────────────────────────────────────────────────────────────

    Given("getMyReviews() — renterId 기준 조회") {

        When("renterId=1인 대여자의 리뷰가 2개 있으면") {
            val (repo, service) = newMocks()
            val reviews = listOf(
                createSavedReview(rentalId = 20L, rating = 5),
                createSavedReview(rentalId = 21L, rating = 4),
            )
            val pageQuery = PageQuery(page = 0, size = 10)

            every { repo.findByRenterId(1L, pageQuery) } returns PageResult(
                content = reviews,
                totalElements = 2L,
                totalPages = 1,
            )

            val result = service.getMyReviews(1L, pageQuery)

            Then("2개의 리뷰가 반환된다") {
                result.content.size shouldBe 2
                result.totalElements shouldBe 2L
            }
        }
    }
})
