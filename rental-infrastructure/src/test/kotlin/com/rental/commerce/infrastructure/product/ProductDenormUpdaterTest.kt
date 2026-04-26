package com.rental.commerce.infrastructure.product

import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalRepository
import com.rental.commerce.domain.common.RentalStatus
import com.rental.commerce.domain.rental.event.RentalStatusChangedEvent
import com.rental.commerce.domain.review.RatingSnapshot
import com.rental.commerce.domain.review.ReviewRepository
import com.rental.commerce.domain.review.event.ReviewCreatedEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal

class ProductDenormUpdaterTest : BehaviorSpec({

    val productRepository = mockk<ProductRepository>()
    val reviewRepository = mockk<ReviewRepository>()
    val rentalRepository = mockk<RentalRepository>()
    val updater = ProductDenormUpdater(productRepository, reviewRepository, rentalRepository)

    beforeEach { clearMocks(productRepository, reviewRepository, rentalRepository) }

    Given("onReviewCreated") {
        When("product 없음") {
            Then("save 미호출") {
                every { productRepository.findById(100L) } returns null
                updater.onReviewCreated(ReviewCreatedEvent(1L, 100L, 9L, 5))
                verify(exactly = 0) { productRepository.save(any()) }
            }
        }

        When("정상 — rating 평균/개수 재계산") {
            Then("applyRatingSnapshot + save 호출") {
                val product = mockk<Product>(relaxed = true)
                every { productRepository.findById(200L) } returns product
                every { reviewRepository.calculateRatingSnapshot(200L) } returns
                    RatingSnapshot(average = BigDecimal("4.50"), count = 3)
                every { productRepository.save(product) } returns product

                updater.onReviewCreated(ReviewCreatedEvent(2L, 200L, 9L, 5))

                verify(exactly = 1) {
                    product.applyRatingSnapshot(BigDecimal("4.50"), 3)
                }
                verify(exactly = 1) { productRepository.save(product) }
            }
        }

        When("repository 예외") {
            Then("로그만 남기고 예외 전파 안 함") {
                every { productRepository.findById(300L) } throws RuntimeException("DB down")
                updater.onReviewCreated(ReviewCreatedEvent(3L, 300L, 9L, 5))
                verify(exactly = 0) { productRepository.save(any()) }
            }
        }
    }

    Given("onRentalReturned") {
        fun event(toStatus: RentalStatus, rentalId: Long = 500L) = RentalStatusChangedEvent(
            rentalId = rentalId,
            renterId = 1L,
            lenderId = 2L,
            fromStatus = RentalStatus.IN_USE,
            toStatus = toStatus,
            rentalAmount = 10000L,
        )

        When("RETURNED 가 아님 (예: PAID)") {
            Then("스킵") {
                updater.onRentalReturned(event(RentalStatus.PAID))
                verify(exactly = 0) { rentalRepository.findById(any()) }
            }
        }

        When("RETURNED 이지만 rental 없음") {
            Then("스킵") {
                every { rentalRepository.findById(600L) } returns null
                updater.onRentalReturned(event(RentalStatus.RETURNED, rentalId = 600L))
                verify(exactly = 0) { productRepository.save(any()) }
            }
        }

        When("정상 흐름") {
            Then("incrementRentalCount + save") {
                val rental = mockk<Rental>()
                every { rental.productId } returns 700L
                every { rentalRepository.findById(500L) } returns rental

                val product = mockk<Product>(relaxed = true)
                every { productRepository.findById(700L) } returns product
                every { productRepository.save(product) } returns product

                updater.onRentalReturned(event(RentalStatus.RETURNED))

                verify(exactly = 1) { product.incrementRentalCount() }
                verify(exactly = 1) { productRepository.save(product) }
            }
        }
    }
})
