package com.rental.commerce.domain.review

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ReviewTest : BehaviorSpec({

    fun validContent() = "상태도 좋고 설명과 동일한 상품이었습니다."

    // ─────────────────────────────────────────────────────────────
    // Review.create() — 정상 생성
    // ─────────────────────────────────────────────────────────────

    Given("Review.create() — 정상 생성") {

        When("유효한 rating(1~5)과 10자 이상 content로 create()를 호출하면") {
            val review = Review.create(
                renterId = 1L,
                rentalId = 10L,
                productId = 100L,
                rating = 5,
                content = validContent(),
            )

            Then("renterId, rentalId, productId가 저장된다") {
                review.renterId shouldBe 1L
                review.rentalId shouldBe 10L
                review.productId shouldBe 100L
            }

            Then("rating이 저장된다") {
                review.rating shouldBe 5
            }

            Then("content가 저장된다") {
                review.content shouldBe validContent()
            }

            Then("createdAt이 null이 아니다") {
                review.createdAt shouldNotBe null
            }
        }

        When("rating이 최솟값(1)이면") {
            val review = Review.create(
                renterId = 1L,
                rentalId = 11L,
                productId = 100L,
                rating = 1,
                content = validContent(),
            )

            Then("rating이 1로 저장된다") {
                review.rating shouldBe 1
            }
        }

        When("content가 정확히 10자이면") {
            val review = Review.create(
                renterId = 1L,
                rentalId = 12L,
                productId = 100L,
                rating = 3,
                content = "1234567890",
            )

            Then("정상 생성된다") {
                review.content shouldBe "1234567890"
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Review.create() — rating 범위 밖 예외
    // ─────────────────────────────────────────────────────────────

    Given("Review.create() — rating 범위 밖 예외") {

        When("rating이 0이면") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Review.create(
                        renterId = 1L,
                        rentalId = 10L,
                        productId = 100L,
                        rating = 0,
                        content = validContent(),
                    )
                }
            }
        }

        When("rating이 6이면") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Review.create(
                        renterId = 1L,
                        rentalId = 10L,
                        productId = 100L,
                        rating = 6,
                        content = validContent(),
                    )
                }
            }
        }

        When("rating이 음수(-1)이면") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Review.create(
                        renterId = 1L,
                        rentalId = 10L,
                        productId = 100L,
                        rating = -1,
                        content = validContent(),
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Review.create() — content 빈 문자열 및 길이 위반
    // ─────────────────────────────────────────────────────────────

    Given("Review.create() — content 검증") {

        When("content가 빈 문자열이면") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Review.create(
                        renterId = 1L,
                        rentalId = 10L,
                        productId = 100L,
                        rating = 5,
                        content = "",
                    )
                }
            }
        }

        When("content가 9자(10자 미만)이면") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Review.create(
                        renterId = 1L,
                        rentalId = 10L,
                        productId = 100L,
                        rating = 5,
                        content = "123456789",
                    )
                }
            }
        }

        When("content가 501자(500자 초과)이면") {
            val longContent = "가".repeat(501)

            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Review.create(
                        renterId = 1L,
                        rentalId = 10L,
                        productId = 100L,
                        rating = 5,
                        content = longContent,
                    )
                }
            }
        }

        When("content가 정확히 500자이면") {
            val maxContent = "가".repeat(500)

            Then("정상 생성된다") {
                val review = Review.create(
                    renterId = 1L,
                    rentalId = 10L,
                    productId = 100L,
                    rating = 5,
                    content = maxContent,
                )
                review.content shouldBe maxContent
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // validateRating() — 직접 호출 검증
    // ─────────────────────────────────────────────────────────────

    Given("validateRating() — 직접 호출") {

        When("유효한 rating(1~5)을 전달하면") {
            Then("예외 없이 통과한다") {
                val review = Review.create(
                    renterId = 1L,
                    rentalId = 10L,
                    productId = 100L,
                    rating = 3,
                    content = validContent(),
                )
                review.validateRating(3)
            }
        }

        When("0을 전달하면") {
            Then("IllegalArgumentException이 발생한다") {
                val review = Review.create(
                    renterId = 1L,
                    rentalId = 10L,
                    productId = 100L,
                    rating = 3,
                    content = validContent(),
                )
                shouldThrow<IllegalArgumentException> {
                    review.validateRating(0)
                }
            }
        }
    }
})
