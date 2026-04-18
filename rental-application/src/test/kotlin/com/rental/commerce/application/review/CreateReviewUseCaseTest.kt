package com.rental.commerce.application.review

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ReviewAlreadyExistsException
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.review.Review
import com.rental.commerce.domain.review.ReviewDomainService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class CreateReviewUseCaseTest : BehaviorSpec({

    val rentalDomainService = mockk<RentalDomainService>()
    val reviewDomainService = mockk<ReviewDomainService>()
    val useCase = CreateReviewUseCase(rentalDomainService, reviewDomainService)

    beforeEach {
        clearMocks(rentalDomainService, reviewDomainService)
    }

    Given("리뷰를 작성할 때") {

        When("정상적으로 리뷰를 작성하면") {
            Then("reviewDomainService.createReview()가 1회 호출된다") {
                val renterId = 1L
                val rentalId = 10L
                val productId = 42L
                val rating = 5
                val content = "상태도 좋고 설명과 동일한 상품이었습니다."

                val rental = mockk<Rental>()
                justRun { rental.verifyRenterAuthority(renterId) }
                justRun { rental.validateReturned() }

                val review = Review.create(
                    renterId = renterId,
                    rentalId = rentalId,
                    productId = productId,
                    rating = rating,
                    content = content,
                )

                val command = CreateReviewCommand(
                    renterId = renterId,
                    rentalId = rentalId,
                    productId = productId,
                    rating = rating,
                    content = content,
                )

                every { rentalDomainService.getRentalById(rentalId) } returns rental
                every {
                    reviewDomainService.createReview(
                        renterId = renterId,
                        rentalId = rentalId,
                        productId = productId,
                        rating = rating,
                        content = content,
                    )
                } returns review

                val result = useCase.execute(command)

                result shouldNotBe null
                result.renterId shouldBe renterId
                result.rentalId shouldBe rentalId
                result.productId shouldBe productId
                result.rating shouldBe rating
                result.content shouldBe content

                verify(exactly = 1) { rentalDomainService.getRentalById(rentalId) }
                verify(exactly = 1) { rental.verifyRenterAuthority(renterId) }
                verify(exactly = 1) { rental.validateReturned() }
                verify(exactly = 1) {
                    reviewDomainService.createReview(
                        renterId = renterId,
                        rentalId = rentalId,
                        productId = productId,
                        rating = rating,
                        content = content,
                    )
                }
            }
        }

        When("대여 상태가 RETURNED가 아닌 경우") {
            Then("REVIEW_RENTAL_NOT_RETURNED 예외가 발생한다") {
                val renterId = 1L
                val rentalId = 10L
                val productId = 42L

                val rental = mockk<Rental>()
                justRun { rental.verifyRenterAuthority(renterId) }
                every { rental.validateReturned() } throws BusinessException(
                    errorCode = ErrorCode.REVIEW_RENTAL_NOT_RETURNED,
                    message = "반납 완료된 대여에만 리뷰를 작성할 수 있습니다.",
                )

                val command = CreateReviewCommand(
                    renterId = renterId,
                    rentalId = rentalId,
                    productId = productId,
                    rating = 5,
                    content = "상태도 좋고 설명과 동일한 상품이었습니다.",
                )

                every { rentalDomainService.getRentalById(rentalId) } returns rental

                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.REVIEW_RENTAL_NOT_RETURNED
            }
        }

        When("본인의 대여가 아닌 경우") {
            Then("REVIEW_FORBIDDEN 예외가 발생한다") {
                val renterId = 1L
                val anotherUserId = 99L
                val rentalId = 10L
                val productId = 42L

                val rental = mockk<Rental>()
                every { rental.verifyRenterAuthority(anotherUserId) } throws BusinessException(
                    errorCode = ErrorCode.REVIEW_FORBIDDEN,
                    message = "본인의 대여에만 리뷰를 작성할 수 있습니다.",
                )

                val command = CreateReviewCommand(
                    renterId = anotherUserId,
                    rentalId = rentalId,
                    productId = productId,
                    rating = 5,
                    content = "상태도 좋고 설명과 동일한 상품이었습니다.",
                )

                every { rentalDomainService.getRentalById(rentalId) } returns rental

                val exception = shouldThrow<BusinessException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.REVIEW_FORBIDDEN
            }
        }

        When("이미 리뷰가 작성된 대여인 경우") {
            Then("ReviewAlreadyExistsException 예외가 발생한다") {
                val renterId = 1L
                val rentalId = 10L
                val productId = 42L

                val rental = mockk<Rental>()
                justRun { rental.verifyRenterAuthority(renterId) }
                justRun { rental.validateReturned() }

                val command = CreateReviewCommand(
                    renterId = renterId,
                    rentalId = rentalId,
                    productId = productId,
                    rating = 5,
                    content = "상태도 좋고 설명과 동일한 상품이었습니다.",
                )

                every { rentalDomainService.getRentalById(rentalId) } returns rental
                every {
                    reviewDomainService.createReview(
                        renterId = renterId,
                        rentalId = rentalId,
                        productId = productId,
                        rating = 5,
                        content = "상태도 좋고 설명과 동일한 상품이었습니다.",
                    )
                } throws ReviewAlreadyExistsException("이미 리뷰가 작성된 대여입니다. rentalId=$rentalId")

                val exception = shouldThrow<ReviewAlreadyExistsException> {
                    useCase.execute(command)
                }
                exception.errorCode shouldBe ErrorCode.REVIEW_ALREADY_EXISTS
            }
        }
    }
})
