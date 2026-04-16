package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.RentalPeriodConflictException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.ZonedDateTime

class RentalDomainServiceTest : BehaviorSpec({

    val rentalRepository = mockk<RentalRepository>()
    val service = RentalDomainService(rentalRepository)

    val productId = 10L
    val now = ZonedDateTime.now()

    // UT-DS01
    Given("기간 중복 없음 — 기존 대여와 겹치지 않는 날짜") {
        When("해당 상품의 활성 대여가 없으면") {
            every {
                rentalRepository.existsOverlappingRental(productId, any(), any())
            } returns false

            Then("유효성 통과 (예외 없음)") {
                shouldNotThrowAny {
                    service.validatePeriodAvailability(
                        productId = productId,
                        startDate = now.plusDays(10),
                        endDate = now.plusDays(17),
                    )
                }
            }
        }
    }

    // UT-DS02
    Given("기간 완전 포함 — 기존 대여 기간에 완전히 포함되는 신청") {
        When("해당 기간에 활성 대여가 존재하면") {
            every {
                rentalRepository.existsOverlappingRental(productId, any(), any())
            } returns true

            Then("RentalPeriodConflictException이 발생한다") {
                shouldThrow<RentalPeriodConflictException> {
                    service.validatePeriodAvailability(
                        productId = productId,
                        startDate = now.plusDays(2),
                        endDate = now.plusDays(5),
                    )
                }
            }
        }
    }

    // UT-DS03
    Given("기간 부분 겹침 (시작) — 새 시작일이 기존 기간 중간에 포함") {
        When("시작일이 기존 대여 기간 중간과 겹치면") {
            every {
                rentalRepository.existsOverlappingRental(productId, any(), any())
            } returns true

            Then("RentalPeriodConflictException이 발생한다") {
                shouldThrow<RentalPeriodConflictException> {
                    service.validatePeriodAvailability(
                        productId = productId,
                        startDate = now.plusDays(3),
                        endDate = now.plusDays(10),
                    )
                }
            }
        }
    }

    // UT-DS04
    Given("기간 부분 겹침 (종료) — 새 종료일이 기존 기간 중간에 포함") {
        When("종료일이 기존 대여 기간 중간과 겹치면") {
            every {
                rentalRepository.existsOverlappingRental(productId, any(), any())
            } returns true

            Then("RentalPeriodConflictException이 발생한다") {
                shouldThrow<RentalPeriodConflictException> {
                    service.validatePeriodAvailability(
                        productId = productId,
                        startDate = now.minusDays(2),
                        endDate = now.plusDays(5),
                    )
                }
            }
        }
    }

    // UT-DS05
    Given("CANCELLED 상태 대여는 무시") {
        When("취소된 대여와 날짜가 겹치더라도 중복 검증에서 무시된다면") {
            every {
                rentalRepository.existsOverlappingRental(productId, any(), any())
            } returns false

            Then("유효성 통과 (예외 없음)") {
                shouldNotThrowAny {
                    service.validatePeriodAvailability(
                        productId = productId,
                        startDate = now.plusDays(1),
                        endDate = now.plusDays(8),
                    )
                }
            }
        }
    }

    // UT-DS06
    Given("일 단위 금액 계산 — 일 5,000원 × 7일") {
        When("일 단가 5000원, 7일 대여를 계산하면") {
            Then("35,000원이 반환된다") {
                val result = service.calculateTotalAmount(
                    dailyPrice = 5_000L,
                    startDate = now,
                    endDate = now.plusDays(7),
                )
                result shouldBe 35_000L
            }
        }
    }

    Given("일 단위 금액 계산 — 소수 없는 정확한 일수 계산") {
        When("시작일과 종료일이 동일하면 (1일 대여)") {
            Then("하루 단가가 반환된다") {
                val result = service.calculateTotalAmount(
                    dailyPrice = 10_000L,
                    startDate = now,
                    endDate = now.plusDays(1),
                )
                result shouldBe 10_000L
            }
        }
    }
})
