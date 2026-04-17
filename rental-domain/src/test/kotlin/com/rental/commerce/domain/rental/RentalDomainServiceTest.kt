package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.common.PaymentFailedException
import com.rental.commerce.domain.common.RentalNotFoundException
import com.rental.commerce.domain.common.RentalPeriodConflictException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime

class RentalDomainServiceTest : BehaviorSpec({

    fun newMocks(): Pair<RentalRepository, RentalDomainService> {
        val repo = mockk<RentalRepository>()
        val rentalPaymentRepository = mockk<RentalPaymentRepository>()
        val paymentGateway = mockk<PaymentGateway>()
        val rentalEventPublisher = mockk<RentalEventPublisher>()
        val rentalQueryRepository = mockk<RentalQueryRepository>()
        return repo to RentalDomainService(
            rentalRepository = repo,
            rentalPaymentRepository = rentalPaymentRepository,
            paymentGateway = paymentGateway,
            rentalEventPublisher = rentalEventPublisher,
            rentalQueryRepository = rentalQueryRepository,
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

    // ──────────────────────────────────────────────────────────────────────────
    // calculateTotalAmount — 추가 엣지 케이스
    // ──────────────────────────────────────────────────────────────────────────

    Given("calculateTotalAmount() — 보증금 0원 엣지 케이스") {

        When("보증금이 0원이고 일 단가 10_000원, 5일인 경우") {
            val (_, service) = newMocks()
            val startDate = ZonedDateTime.now().plusDays(1)
            val endDate = startDate.plusDays(5)

            Then("결과는 10_000 × 5 = 50_000원이다") {
                val result = service.calculateTotalAmount(
                    dailyPrice = 10_000L,
                    startDate = startDate,
                    endDate = endDate,
                    depositAmount = 0L,
                )
                result shouldBe 50_000L
            }
        }
    }

    Given("calculateTotalAmount() — 일 단가 0원 엣지 케이스") {

        When("일 단가가 0원이고 보증금 30_000원인 경우") {
            val (_, service) = newMocks()
            val startDate = ZonedDateTime.now().plusDays(1)
            val endDate = startDate.plusDays(7)

            Then("결과는 0 × 7 + 30_000 = 30_000원이다") {
                val result = service.calculateTotalAmount(
                    dailyPrice = 0L,
                    startDate = startDate,
                    endDate = endDate,
                    depositAmount = 30_000L,
                )
                result shouldBe 30_000L
            }
        }
    }

    Given("calculateTotalAmount() — 1일 최소 기간") {

        When("기간이 정확히 1일(startDate + 1일 = endDate)인 경우") {
            val (_, service) = newMocks()
            val startDate = ZonedDateTime.now().plusDays(1)
            val endDate = startDate.plusDays(1)

            Then("결과는 dailyPrice × 1 + depositAmount이다") {
                val result = service.calculateTotalAmount(
                    dailyPrice = 15_000L,
                    startDate = startDate,
                    endDate = endDate,
                    depositAmount = 10_000L,
                )
                result shouldBe 25_000L
            }
        }
    }

    Given("calculateTotalAmount() — 최대 기간 30일") {

        When("기간이 30일이고 일 단가 10_000원, 보증금 100_000원인 경우") {
            val (_, service) = newMocks()
            val startDate = ZonedDateTime.now().plusDays(1)
            val endDate = startDate.plusDays(30)

            Then("결과는 10_000 × 30 + 100_000 = 400_000원이다") {
                val result = service.calculateTotalAmount(
                    dailyPrice = 10_000L,
                    startDate = startDate,
                    endDate = endDate,
                    depositAmount = 100_000L,
                )
                result shouldBe 400_000L
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // validatePeriodAvailability — 추가 엣지 케이스
    // ──────────────────────────────────────────────────────────────────────────

    Given("validatePeriodAvailability() — excludeRentalId 적용") {

        When("동일 rentalId를 excludeRentalId로 전달하면 중복 아닌 것으로 처리된다") {
            val (repo, service) = newMocks()
            val productId = 10L
            val startDate = ZonedDateTime.now().plusDays(1)
            val endDate = ZonedDateTime.now().plusDays(7)
            val excludeRentalId = 99L

            every {
                repo.existsOverlappingRental(productId, startDate, endDate, excludeRentalId)
            } returns false

            Then("예외 없이 통과한다") {
                service.validatePeriodAvailability(productId, startDate, endDate, excludeRentalId)

                verify(exactly = 1) {
                    repo.existsOverlappingRental(productId, startDate, endDate, excludeRentalId)
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getRentalsByUserId
    // ──────────────────────────────────────────────────────────────────────────

    Given("getRentalsByUserId() — 사용자의 대여 목록 조회") {

        When("userId가 renterId 또는 lenderId인 대여가 2건 있는 경우") {
            val (repo, service) = newMocks()
            val userId = 1L
            val rental1 = createRental(renterId = userId, lenderId = 2L)
            val rental2 = createRental(renterId = 3L, lenderId = userId)

            every { repo.findAllByRenterIdOrLenderId(userId, userId) } returns listOf(rental1, rental2)

            Then("2건의 Rental 리스트를 반환한다") {
                val result = service.getRentalsByUserId(userId)
                result.size shouldBe 2
                verify(exactly = 1) { repo.findAllByRenterIdOrLenderId(userId, userId) }
            }
        }

        When("해당 userId가 참여한 대여가 없는 경우") {
            val (repo, service) = newMocks()
            val userId = 999L

            every { repo.findAllByRenterIdOrLenderId(userId, userId) } returns emptyList()

            Then("빈 리스트를 반환한다") {
                val result = service.getRentalsByUserId(userId)
                result shouldBe emptyList()
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // approveRental
    // ──────────────────────────────────────────────────────────────────────────

    Given("approveRental() — 등록자 권한 검증 + approve + save + 이벤트 발행") {

        When("lenderId와 일치하는 userId로 approveRental()을 호출하면") {
            val rr = mockk<RentalRepository>()
            val rpr = mockk<RentalPaymentRepository>()
            val pg = mockk<PaymentGateway>()
            val rep = mockk<RentalEventPublisher>()
            val rqr = mockk<RentalQueryRepository>()
            val svc = RentalDomainService(
                rentalRepository = rr,
                rentalPaymentRepository = rpr,
                paymentGateway = pg,
                rentalEventPublisher = rep,
                rentalQueryRepository = rqr,
            )
            val rental = createRental(lenderId = 2L)
            every { rr.save(any()) } returns rental
            justRun { rep.publishAll(any()) }

            Then("APPROVED 상태로 전이되고 save가 호출된다") {
                val result = svc.approveRental(rental, 2L)
                result.status shouldBe RentalStatus.APPROVED
                verify(exactly = 1) { rr.save(any()) }
                verify(exactly = 1) { rep.publishAll(any()) }
            }
        }

        When("lenderId와 다른 userId로 approveRental()을 호출하면") {
            val rr = mockk<RentalRepository>()
            val rpr = mockk<RentalPaymentRepository>()
            val pg = mockk<PaymentGateway>()
            val rep = mockk<RentalEventPublisher>()
            val rqr = mockk<RentalQueryRepository>()
            val svc = RentalDomainService(rr, rpr, pg, rep, rqr)
            val rental = createRental(lenderId = 2L)

            Then("BusinessException(FORBIDDEN)이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    svc.approveRental(rental, 999L)
                }
                exception.errorCode shouldBe ErrorCode.FORBIDDEN
                verify(exactly = 0) { rr.save(any()) }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // rejectRental
    // ──────────────────────────────────────────────────────────────────────────

    Given("rejectRental() — 등록자 권한 검증 + reject + save + 이벤트 발행") {

        When("lenderId와 일치하는 userId로 rejectRental()을 호출하면") {
            val rr = mockk<RentalRepository>()
            val rpr = mockk<RentalPaymentRepository>()
            val pg = mockk<PaymentGateway>()
            val rep = mockk<RentalEventPublisher>()
            val rqr = mockk<RentalQueryRepository>()
            val svc = RentalDomainService(rr, rpr, pg, rep, rqr)
            val rental = createRental(lenderId = 2L)
            every { rr.save(any()) } returns rental
            justRun { rep.publishAll(any()) }

            Then("CANCELLED 상태로 전이되고 cancelReason이 설정된다") {
                val result = svc.rejectRental(rental, 2L, "일정 충돌")
                result.status shouldBe RentalStatus.CANCELLED
                result.cancelReason shouldBe "일정 충돌"
                verify(exactly = 1) { rr.save(any()) }
            }
        }

        When("lenderId와 다른 userId로 rejectRental()을 호출하면") {
            val rr = mockk<RentalRepository>()
            val rpr = mockk<RentalPaymentRepository>()
            val pg = mockk<PaymentGateway>()
            val rep = mockk<RentalEventPublisher>()
            val rqr = mockk<RentalQueryRepository>()
            val svc = RentalDomainService(rr, rpr, pg, rep, rqr)
            val rental = createRental(lenderId = 2L)

            Then("BusinessException(FORBIDDEN)이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    svc.rejectRental(rental, 999L, "거절 사유")
                }
                exception.errorCode shouldBe ErrorCode.FORBIDDEN
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // startRental
    // ──────────────────────────────────────────────────────────────────────────

    Given("startRental() — 등록자 권한 검증 + startRental + save + 이벤트 발행") {

        When("lenderId와 일치하는 userId로 startRental()을 호출하면") {
            val rr = mockk<RentalRepository>()
            val rpr = mockk<RentalPaymentRepository>()
            val pg = mockk<PaymentGateway>()
            val rep = mockk<RentalEventPublisher>()
            val rqr = mockk<RentalQueryRepository>()
            val svc = RentalDomainService(rr, rpr, pg, rep, rqr)
            val rental = createRental(lenderId = 2L)
            rental.approve()
            rental.pullEvents()
            rental.markPaid()
            rental.pullEvents()
            every { rr.save(any()) } returns rental
            justRun { rep.publishAll(any()) }

            Then("IN_USE 상태로 전이된다") {
                val result = svc.startRental(rental, 2L)
                result.status shouldBe RentalStatus.IN_USE
                verify(exactly = 1) { rr.save(any()) }
            }
        }

        When("lenderId와 다른 userId로 startRental()을 호출하면") {
            val rr = mockk<RentalRepository>()
            val rpr = mockk<RentalPaymentRepository>()
            val pg = mockk<PaymentGateway>()
            val rep = mockk<RentalEventPublisher>()
            val rqr = mockk<RentalQueryRepository>()
            val svc = RentalDomainService(rr, rpr, pg, rep, rqr)
            val rental = createRental(lenderId = 2L)
            rental.approve()
            rental.pullEvents()
            rental.markPaid()
            rental.pullEvents()

            Then("BusinessException(FORBIDDEN)이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    svc.startRental(rental, 999L)
                }
                exception.errorCode shouldBe ErrorCode.FORBIDDEN
                verify(exactly = 0) { rr.save(any()) }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // returnRental
    // ──────────────────────────────────────────────────────────────────────────

    Given("returnRental() — IN_USE 상태에서 RETURNED로 전이") {

        When("IN_USE 상태의 대여에 returnRental()을 호출하면") {
            val rr = mockk<RentalRepository>()
            val rpr = mockk<RentalPaymentRepository>()
            val pg = mockk<PaymentGateway>()
            val rep = mockk<RentalEventPublisher>()
            val rqr = mockk<RentalQueryRepository>()
            val svc = RentalDomainService(rr, rpr, pg, rep, rqr)
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            rental.markPaid()
            rental.pullEvents()
            rental.startRental()
            rental.pullEvents()
            every { rr.save(any()) } returns rental
            justRun { rep.publishAll(any()) }

            Then("RETURNED 상태로 전이되고 save가 호출된다") {
                val result = svc.returnRental(rental)
                result.status shouldBe RentalStatus.RETURNED
                verify(exactly = 1) { rr.save(any()) }
                verify(exactly = 1) { rep.publishAll(any()) }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // cancelRental
    // ──────────────────────────────────────────────────────────────────────────

    Given("cancelRental() — REQUESTED 상태 취소 (환불 없음)") {

        When("REQUESTED 상태의 대여를 cancelRental()로 취소하면") {
            val rr = mockk<RentalRepository>()
            val rpr = mockk<RentalPaymentRepository>()
            val pg = mockk<PaymentGateway>()
            val rep = mockk<RentalEventPublisher>()
            val rqr = mockk<RentalQueryRepository>()
            val svc = RentalDomainService(rr, rpr, pg, rep, rqr)
            val rental = createRental()
            every { rr.save(any()) } returns rental
            justRun { rep.publishAll(any()) }

            Then("CANCELLED 상태로 전이되고 환불 처리(paymentGateway.cancelPayment)는 호출되지 않는다") {
                val result = svc.cancelRental(rental, "변심")
                result.status shouldBe RentalStatus.CANCELLED
                verify(exactly = 0) { pg.cancelPayment(any(), any()) }
                verify(exactly = 1) { rr.save(any()) }
            }
        }
    }

    Given("cancelRental() — PAID 상태 취소 (결제 없는 경우)") {

        When("PAID 상태이지만 RentalPayment가 없는 경우 cancelRental()을 호출하면") {
            val rr = mockk<RentalRepository>()
            val rpr = mockk<RentalPaymentRepository>()
            val pg = mockk<PaymentGateway>()
            val rep = mockk<RentalEventPublisher>()
            val rqr = mockk<RentalQueryRepository>()
            val svc = RentalDomainService(rr, rpr, pg, rep, rqr)
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            rental.markPaid()
            rental.pullEvents()
            every { rpr.findByRentalId(rental.id) } returns null
            every { rr.save(any()) } returns rental
            justRun { rep.publishAll(any()) }

            Then("환불 없이 CANCELLED 상태로 전이된다") {
                val result = svc.cancelRental(rental, "결제 취소")
                result.status shouldBe RentalStatus.CANCELLED
                verify(exactly = 0) { pg.cancelPayment(any(), any()) }
            }
        }
    }

    Given("cancelRental() — PAID 상태 취소 (완료된 결제 환불)") {

        When("PAID 상태에 COMPLETED 결제가 있는 경우 cancelRental()을 호출하면") {
            val rr = mockk<RentalRepository>()
            val rpr = mockk<RentalPaymentRepository>()
            val pg = mockk<PaymentGateway>()
            val rep = mockk<RentalEventPublisher>()
            val rqr = mockk<RentalQueryRepository>()
            val svc = RentalDomainService(rr, rpr, pg, rep, rqr)
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            rental.markPaid()
            rental.pullEvents()
            val payment = RentalPayment.create(
                rentalId = rental.id,
                amount = 70_000L,
                paymentMethod = PaymentMethod.CARD,
                orderId = "RC-0001",
            )
            payment.complete("ext-key-001")

            every { rpr.findByRentalId(rental.id) } returns payment
            every { pg.cancelPayment("ext-key-001", "결제 후 취소") } returns PaymentCancelResult(
                success = true,
                cancelAmount = 70_000L,
                canceledAt = ZonedDateTime.now(),
            )
            every { rpr.save(any()) } returns payment
            every { rr.save(any()) } returns rental
            justRun { rep.publishAll(any()) }

            Then("paymentGateway.cancelPayment가 호출되고 payment가 REFUNDED로 전이된다") {
                val result = svc.cancelRental(rental, "결제 후 취소")
                result.status shouldBe RentalStatus.CANCELLED
                payment.status shouldBe PaymentStatus.REFUNDED
                verify(exactly = 1) { pg.cancelPayment("ext-key-001", "결제 후 취소") }
                verify(exactly = 1) { rpr.save(any()) }
            }
        }
    }

    Given("cancelRental() — PAID 상태 취소 (PENDING 결제 — 환불 불필요)") {

        When("PAID 상태에 PENDING 결제(미완료)가 있는 경우 cancelRental()을 호출하면") {
            val rr = mockk<RentalRepository>()
            val rpr = mockk<RentalPaymentRepository>()
            val pg = mockk<PaymentGateway>()
            val rep = mockk<RentalEventPublisher>()
            val rqr = mockk<RentalQueryRepository>()
            val svc = RentalDomainService(rr, rpr, pg, rep, rqr)
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            rental.markPaid()
            rental.pullEvents()
            val pendingPayment = RentalPayment.create(
                rentalId = rental.id,
                amount = 70_000L,
                paymentMethod = PaymentMethod.CARD,
                orderId = "RC-0001",
            )

            every { rpr.findByRentalId(rental.id) } returns pendingPayment
            every { rr.save(any()) } returns rental
            justRun { rep.publishAll(any()) }

            Then("paymentGateway.cancelPayment가 호출되지 않는다") {
                svc.cancelRental(rental, "취소")
                verify(exactly = 0) { pg.cancelPayment(any(), any()) }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getRentalDetail
    // ──────────────────────────────────────────────────────────────────────────

    Given("getRentalDetail() — 대여 상세 조회 (Rental + RentalPayment)") {

        When("존재하는 rentalId로 getRentalDetail()을 호출하면") {
            val rr = mockk<RentalRepository>()
            val rpr = mockk<RentalPaymentRepository>()
            val pg = mockk<PaymentGateway>()
            val rep = mockk<RentalEventPublisher>()
            val rqr = mockk<RentalQueryRepository>()
            val svc = RentalDomainService(rr, rpr, pg, rep, rqr)
            val rentalId = 1L
            val rental = createRental()
            val payment = RentalPayment.create(
                rentalId = rentalId,
                amount = 70_000L,
                paymentMethod = PaymentMethod.CARD,
                orderId = "RC-0001",
            )

            every { rqr.findRentalWithPayment(rentalId) } returns RentalWithPayment(rental = rental, payment = payment)

            Then("RentalWithPayment가 반환된다") {
                val result = svc.getRentalDetail(rentalId)
                result.rental shouldBe rental
                result.payment shouldBe payment
                verify(exactly = 1) { rqr.findRentalWithPayment(rentalId) }
            }
        }

        When("존재하지 않는 rentalId로 getRentalDetail()을 호출하면") {
            val rr = mockk<RentalRepository>()
            val rpr = mockk<RentalPaymentRepository>()
            val pg = mockk<PaymentGateway>()
            val rep = mockk<RentalEventPublisher>()
            val rqr = mockk<RentalQueryRepository>()
            val svc = RentalDomainService(rr, rpr, pg, rep, rqr)
            val rentalId = 999L

            every { rqr.findRentalWithPayment(rentalId) } returns null

            Then("RentalNotFoundException이 발생한다") {
                shouldThrow<RentalNotFoundException> {
                    svc.getRentalDetail(rentalId)
                }
            }
        }

        When("결제 정보 없이 조회하면 payment가 null인 RentalWithPayment가 반환된다") {
            val rr = mockk<RentalRepository>()
            val rpr = mockk<RentalPaymentRepository>()
            val pg = mockk<PaymentGateway>()
            val rep = mockk<RentalEventPublisher>()
            val rqr = mockk<RentalQueryRepository>()
            val svc = RentalDomainService(rr, rpr, pg, rep, rqr)
            val rentalId = 2L
            val rental = createRental()

            every { rqr.findRentalWithPayment(rentalId) } returns RentalWithPayment(rental = rental, payment = null)

            Then("payment가 null인 RentalWithPayment가 반환된다") {
                val result = svc.getRentalDetail(rentalId)
                result.rental shouldBe rental
                result.payment shouldBe null
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getMyRentals
    // ──────────────────────────────────────────────────────────────────────────

    Given("getMyRentals() — 내 대여 목록 페이지네이션 조회") {

        When("userId와 pageQuery로 getMyRentals()를 호출하면") {
            val rr = mockk<RentalRepository>()
            val rpr = mockk<RentalPaymentRepository>()
            val pg = mockk<PaymentGateway>()
            val rep = mockk<RentalEventPublisher>()
            val rqr = mockk<RentalQueryRepository>()
            val svc = RentalDomainService(rr, rpr, pg, rep, rqr)
            val userId = 10L
            val rental1 = createRental(renterId = userId)
            val rental2 = createRental(renterId = userId)
            val condition = RentalQueryCondition(
                userId = userId,
                statusFilter = null,
                pageQuery = PageQuery(page = 0, size = 20),
            )

            every { rqr.findMyRentals(condition) } returns PageResult(
                content = listOf(rental1, rental2),
                totalElements = 2L,
                totalPages = 1,
            )

            Then("PageResult가 반환된다") {
                val result = svc.getMyRentals(condition)
                result.content.size shouldBe 2
                result.totalElements shouldBe 2L
                verify(exactly = 1) { rqr.findMyRentals(condition) }
            }
        }

        When("결과가 없으면 빈 PageResult가 반환된다") {
            val rr = mockk<RentalRepository>()
            val rpr = mockk<RentalPaymentRepository>()
            val pg = mockk<PaymentGateway>()
            val rep = mockk<RentalEventPublisher>()
            val rqr = mockk<RentalQueryRepository>()
            val svc = RentalDomainService(rr, rpr, pg, rep, rqr)
            val condition = RentalQueryCondition(userId = 999L)

            every { rqr.findMyRentals(condition) } returns PageResult(
                content = emptyList(),
                totalElements = 0L,
                totalPages = 0,
            )

            Then("빈 content의 PageResult가 반환된다") {
                val result = svc.getMyRentals(condition)
                result.content shouldBe emptyList()
                result.totalElements shouldBe 0L
            }
        }
    }
})
