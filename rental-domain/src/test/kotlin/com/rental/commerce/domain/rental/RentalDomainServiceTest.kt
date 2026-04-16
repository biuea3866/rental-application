package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.RentalNotFoundException
import com.rental.commerce.domain.common.RentalPeriodConflictException
import com.rental.commerce.domain.product.ProductRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime

class RentalDomainServiceTest : BehaviorSpec({

    fun newMocks(): Pair<RentalRepository, RentalDomainService> {
        val repo = mockk<RentalRepository>()
        val rentalPaymentRepository = mockk<RentalPaymentRepository>()
        val paymentGateway = mockk<PaymentGateway>()
        val rentalEventPublisher = mockk<RentalEventPublisher>()
        val productRepository = mockk<ProductRepository>()
        return repo to RentalDomainService(
            rentalRepository = repo,
            rentalPaymentRepository = rentalPaymentRepository,
            paymentGateway = paymentGateway,
            rentalEventPublisher = rentalEventPublisher,
            productRepository = productRepository,
        )
    }

    fun createRental(
        renterId: Long = 1L,
        lenderId: Long = 2L,
        productId: Long = 10L,
        startDate: ZonedDateTime = ZonedDateTime.now().plusDays(1),
        endDate: ZonedDateTime = ZonedDateTime.now().plusDays(7),
        totalAmount: Long = 70_000L,
        depositAmount: Long = 50_000L,
    ): Rental = Rental.create(
        renterId = renterId,
        lenderId = lenderId,
        productId = productId,
        startDate = startDate,
        endDate = endDate,
        totalAmount = totalAmount,
        depositAmount = depositAmount,
        deliveryInfo = DeliveryInfo(
            recipientName = "홍길동",
            recipientPhone = "010-1234-5678",
            addressLine1 = "서울특별시 강남구 테헤란로 123",
            addressLine2 = "101호",
            zipCode = "06234",
        ),
    )

    // ──────────────────────────────────────────────────────────────────────────
    // validatePeriodAvailability
    // ──────────────────────────────────────────────────────────────────────────

    Given("validatePeriodAvailability() — 기간 중복 없음") {

        When("해당 기간에 겹치는 대여가 없을 때") {
            val (repo, service) = newMocks()
            val productId = 10L
            val startDate = ZonedDateTime.now().plusDays(1)
            val endDate = ZonedDateTime.now().plusDays(7)

            every {
                repo.existsOverlappingRental(productId, startDate, endDate, null)
            } returns false

            Then("예외 없이 통과한다") {
                service.validatePeriodAvailability(productId, startDate, endDate)

                verify(exactly = 1) {
                    repo.existsOverlappingRental(productId, startDate, endDate, null)
                }
            }
        }
    }

    Given("validatePeriodAvailability() — 기간 중복 있음") {

        When("해당 기간에 겹치는 대여가 존재할 때") {
            val (repo, service) = newMocks()
            val productId = 10L
            val startDate = ZonedDateTime.now().plusDays(1)
            val endDate = ZonedDateTime.now().plusDays(7)

            every {
                repo.existsOverlappingRental(productId, startDate, endDate, null)
            } returns true

            Then("RentalPeriodConflictException이 발생한다") {
                shouldThrow<RentalPeriodConflictException> {
                    service.validatePeriodAvailability(productId, startDate, endDate)
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // calculateTotalAmount
    // ──────────────────────────────────────────────────────────────────────────

    Given("calculateTotalAmount() — 일 단가 × 일수 + 보증금 계산") {

        When("일 단가 10_000원, 7일, 보증금 50_000원으로 계산하면") {
            val (_, service) = newMocks()
            val dailyPrice = 10_000L
            val startDate = ZonedDateTime.now().plusDays(1)
            val endDate = startDate.plusDays(7)
            val depositAmount = 50_000L

            Then("결과는 (10_000 × 7) + 50_000 = 120_000원이다") {
                val result = service.calculateTotalAmount(
                    dailyPrice = dailyPrice,
                    startDate = startDate,
                    endDate = endDate,
                    depositAmount = depositAmount,
                )
                result shouldBe 120_000L
            }
        }

        When("일 단가 5_000원, 3일, 보증금 20_000원으로 계산하면") {
            val (_, service) = newMocks()
            val dailyPrice = 5_000L
            val startDate = ZonedDateTime.now().plusDays(1)
            val endDate = startDate.plusDays(3)
            val depositAmount = 20_000L

            Then("결과는 (5_000 × 3) + 20_000 = 35_000원이다") {
                val result = service.calculateTotalAmount(
                    dailyPrice = dailyPrice,
                    startDate = startDate,
                    endDate = endDate,
                    depositAmount = depositAmount,
                )
                result shouldBe 35_000L
            }
        }
    }

    Given("calculateTotalAmount() — 0일 또는 음수 기간 예외") {

        When("startDate와 endDate가 동일(0일)한 경우") {
            val (_, service) = newMocks()
            val dailyPrice = 10_000L
            val startDate = ZonedDateTime.now().plusDays(1)
            val endDate = startDate
            val depositAmount = 50_000L

            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    service.calculateTotalAmount(
                        dailyPrice = dailyPrice,
                        startDate = startDate,
                        endDate = endDate,
                        depositAmount = depositAmount,
                    )
                }
            }
        }

        When("endDate가 startDate보다 이전(음수)인 경우") {
            val (_, service) = newMocks()
            val dailyPrice = 10_000L
            val startDate = ZonedDateTime.now().plusDays(5)
            val endDate = ZonedDateTime.now().plusDays(1)
            val depositAmount = 50_000L

            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    service.calculateTotalAmount(
                        dailyPrice = dailyPrice,
                        startDate = startDate,
                        endDate = endDate,
                        depositAmount = depositAmount,
                    )
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getRentalById
    // ──────────────────────────────────────────────────────────────────────────

    Given("getRentalById() — 대여 조회") {

        When("존재하는 rentalId로 조회하면") {
            val (repo, service) = newMocks()
            val rentalId = 1L
            val rental = createRental()

            every { repo.findById(rentalId) } returns rental

            Then("해당 Rental을 반환한다") {
                val result = service.getRentalById(rentalId)
                result shouldNotBe null
                result shouldBe rental
            }
        }

        When("존재하지 않는 rentalId로 조회하면") {
            val (repo, service) = newMocks()
            val rentalId = 999L

            every { repo.findById(rentalId) } returns null

            Then("RentalNotFoundException이 발생한다") {
                shouldThrow<RentalNotFoundException> {
                    service.getRentalById(rentalId)
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // createRental
    // ──────────────────────────────────────────────────────────────────────────

    Given("createRental() — 대여 생성 + 기간 검증 포함") {

        When("기간 중복이 없는 경우 createRental()을 호출하면") {
            val (repo, service) = newMocks()
            val productId = 10L
            val startDate = ZonedDateTime.now().plusDays(1)
            val endDate = ZonedDateTime.now().plusDays(7)
            val rental = createRental(
                productId = productId,
                startDate = startDate,
                endDate = endDate,
            )

            every {
                repo.existsOverlappingRental(productId, startDate, endDate, null)
            } returns false
            every { repo.save(any()) } returns rental

            Then("Rental.create()가 호출되고 저장된 Rental을 반환한다") {
                val result = service.createRental(
                    renterId = rental.renterId,
                    lenderId = rental.lenderId,
                    productId = productId,
                    startDate = startDate,
                    endDate = endDate,
                    dailyPrice = 10_000L,
                    depositAmount = rental.depositAmount,
                    deliveryInfo = rental.deliveryInfo,
                )

                result shouldNotBe null
                result.productId shouldBe productId
                result.renterId shouldBe rental.renterId

                verify(exactly = 1) {
                    repo.existsOverlappingRental(productId, startDate, endDate, null)
                }
                verify(exactly = 1) { repo.save(any()) }
            }
        }

        When("기간 중복이 있는 경우 createRental()을 호출하면") {
            val (repo, service) = newMocks()
            val productId = 10L
            val startDate = ZonedDateTime.now().plusDays(1)
            val endDate = ZonedDateTime.now().plusDays(7)

            every {
                repo.existsOverlappingRental(productId, startDate, endDate, null)
            } returns true

            Then("RentalPeriodConflictException이 발생하고 save는 호출되지 않는다") {
                shouldThrow<RentalPeriodConflictException> {
                    service.createRental(
                        renterId = 1L,
                        lenderId = 2L,
                        productId = productId,
                        startDate = startDate,
                        endDate = endDate,
                        dailyPrice = 10_000L,
                        depositAmount = 50_000L,
                        deliveryInfo = DeliveryInfo(
                            recipientName = "홍길동",
                            recipientPhone = "010-1234-5678",
                            addressLine1 = "서울특별시 강남구",
                            zipCode = "06234",
                        ),
                    )
                }

                verify(exactly = 0) { repo.save(any()) }
            }
        }
    }
})
