package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.rental.event.RentalStatusChangedEvent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.ZonedDateTime

class RentalTest : BehaviorSpec({

    fun createRequestedRental(
        renterId: Long = 1L,
        lenderId: Long = 2L,
        productId: Long = 10L,
        totalAmount: Long = 35_000L,
        depositAmount: Long = 50_000L,
    ): Rental {
        return Rental.request(
            renterId = renterId,
            lenderId = lenderId,
            productId = productId,
            startDate = ZonedDateTime.now().plusDays(1),
            endDate = ZonedDateTime.now().plusDays(8),
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
    }

    // UT-R01
    Given("정상 대여 신청 — Rental.request()") {
        When("유효한 정보로 대여 신청을 생성하면") {
            val rental = createRequestedRental()

            Then("status가 REQUESTED이다") {
                rental.status shouldBe RentalStatus.REQUESTED
            }

            Then("requestedAt이 설정된다") {
                rental.requestedAt shouldNotBe null
            }

            Then("RentalStatusChangedEvent가 발행된다") {
                val events = rental.pullEvents()
                events shouldHaveSize 1
                events.first().shouldBeInstanceOf<RentalStatusChangedEvent>()
                val event = events.first() as RentalStatusChangedEvent
                event.newStatus shouldBe RentalStatus.REQUESTED
            }
        }
    }

    // UT-R02
    Given("승인 처리 — REQUESTED → approve()") {
        When("REQUESTED 상태에서 approve()를 호출하면") {
            val rental = createRequestedRental()
            rental.pullEvents() // clear
            rental.approve()

            Then("status가 APPROVED이다") {
                rental.status shouldBe RentalStatus.APPROVED
            }

            Then("approvedAt이 설정된다") {
                rental.approvedAt shouldNotBe null
            }

            Then("RentalStatusChangedEvent(APPROVED)가 발행된다") {
                val events = rental.pullEvents()
                events shouldHaveSize 1
                val event = events.first() as RentalStatusChangedEvent
                event.prevStatus shouldBe RentalStatus.REQUESTED
                event.newStatus shouldBe RentalStatus.APPROVED
            }
        }
    }

    // UT-R03
    Given("거절 처리 — REQUESTED → reject(reason)") {
        When("REQUESTED 상태에서 reject()를 호출하면") {
            val rental = createRequestedRental()
            rental.pullEvents()
            rental.reject("해당 기간에 다른 일정이 생겼습니다.")

            Then("status가 CANCELLED이다") {
                rental.status shouldBe RentalStatus.CANCELLED
            }

            Then("cancelReason이 설정된다") {
                rental.cancelReason shouldBe "해당 기간에 다른 일정이 생겼습니다."
            }

            Then("cancelledAt이 설정된다") {
                rental.cancelledAt shouldNotBe null
            }

            Then("RentalStatusChangedEvent(CANCELLED)가 발행된다") {
                val events = rental.pullEvents()
                events shouldHaveSize 1
                val event = events.first() as RentalStatusChangedEvent
                event.newStatus shouldBe RentalStatus.CANCELLED
            }
        }
    }

    // UT-R04
    Given("결제 완료 — APPROVED → markPaid()") {
        When("APPROVED 상태에서 markPaid()를 호출하면") {
            val rental = createRequestedRental()
            rental.pullEvents()
            rental.approve()
            rental.pullEvents()
            rental.markPaid()

            Then("status가 PAID이다") {
                rental.status shouldBe RentalStatus.PAID
            }

            Then("paidAt이 설정된다") {
                rental.paidAt shouldNotBe null
            }

            Then("RentalStatusChangedEvent(PAID)가 발행된다") {
                val events = rental.pullEvents()
                events shouldHaveSize 1
                val event = events.first() as RentalStatusChangedEvent
                event.newStatus shouldBe RentalStatus.PAID
            }
        }
    }

    // UT-R05
    Given("대여 시작 — PAID → startRental()") {
        When("PAID 상태에서 startRental()을 호출하면") {
            val rental = createRequestedRental()
            rental.pullEvents()
            rental.approve()
            rental.markPaid()
            rental.pullEvents()
            rental.startRental()

            Then("status가 IN_USE이다") {
                rental.status shouldBe RentalStatus.IN_USE
            }

            Then("startedAt이 설정된다") {
                rental.startedAt shouldNotBe null
            }

            Then("RentalStatusChangedEvent(IN_USE)가 발행된다") {
                val events = rental.pullEvents()
                events shouldHaveSize 1
                val event = events.first() as RentalStatusChangedEvent
                event.newStatus shouldBe RentalStatus.IN_USE
            }
        }
    }

    // UT-R06
    Given("반납 처리 — IN_USE → returnRental()") {
        When("IN_USE 상태에서 returnRental()을 호출하면") {
            val rental = createRequestedRental()
            rental.pullEvents()
            rental.approve()
            rental.markPaid()
            rental.startRental()
            rental.pullEvents()
            rental.returnRental()

            Then("status가 RETURNED이다") {
                rental.status shouldBe RentalStatus.RETURNED
            }

            Then("returnedAt이 설정된다") {
                rental.returnedAt shouldNotBe null
            }

            Then("RentalStatusChangedEvent(RETURNED)가 발행된다") {
                val events = rental.pullEvents()
                events shouldHaveSize 1
                val event = events.first() as RentalStatusChangedEvent
                event.newStatus shouldBe RentalStatus.RETURNED
            }
        }
    }

    // UT-R07
    Given("취소 처리 (REQUESTED) — REQUESTED → cancel()") {
        When("REQUESTED 상태에서 cancel()을 호출하면") {
            val rental = createRequestedRental()
            rental.pullEvents()
            rental.cancel("일정이 변경되었습니다.")

            Then("status가 CANCELLED이다") {
                rental.status shouldBe RentalStatus.CANCELLED
            }

            Then("cancelReason이 설정된다") {
                rental.cancelReason shouldBe "일정이 변경되었습니다."
            }
        }
    }

    // UT-R08
    Given("취소 처리 (APPROVED) — APPROVED → cancel()") {
        When("APPROVED 상태에서 cancel()을 호출하면") {
            val rental = createRequestedRental()
            rental.pullEvents()
            rental.approve()
            rental.pullEvents()
            rental.cancel("일정이 변경되었습니다.")

            Then("status가 CANCELLED이다") {
                rental.status shouldBe RentalStatus.CANCELLED
            }
        }
    }

    // UT-R09
    Given("불법 전이 차단 — PAID에서 approve()") {
        When("PAID 상태에서 approve()를 호출하면") {
            val rental = createRequestedRental()
            rental.approve()
            rental.markPaid()
            rental.pullEvents()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    rental.approve()
                }
            }
        }
    }

    // UT-R10
    Given("불법 전이 차단 — RETURNED에서 cancel()") {
        When("RETURNED 상태에서 cancel()을 호출하면") {
            val rental = createRequestedRental()
            rental.approve()
            rental.markPaid()
            rental.startRental()
            rental.returnRental()
            rental.pullEvents()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    rental.cancel("취소 사유")
                }
            }
        }
    }

    // UT-R11
    Given("불법 전이 차단 — CANCELLED에서 approve()") {
        When("CANCELLED 상태에서 approve()를 호출하면") {
            val rental = createRequestedRental()
            rental.cancel("취소 사유")
            rental.pullEvents()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    rental.approve()
                }
            }
        }
    }

    // UT-R12
    Given("동일 상태 멱등 처리 — APPROVED에서 approve() 재호출") {
        When("APPROVED 상태에서 approve()를 다시 호출하면") {
            val rental = createRequestedRental()
            rental.approve()
            rental.pullEvents()

            Then("에러 없이 현재 상태 APPROVED를 유지한다") {
                rental.approve()
                rental.status shouldBe RentalStatus.APPROVED
            }
        }
    }

    // BLK-001: JPA 리플렉션 null 방어
    Given("JPA 리플렉션으로 로드된 Rental — domainEvents null 시뮬레이션") {
        fun createRentalWithNullDomainEvents(): Rental {
            val rental = createRequestedRental()
            val field = Rental::class.java.getDeclaredField("_domainEvents")
            field.isAccessible = true
            field.set(rental, null)
            return rental
        }

        When("domainEvents가 null인 상태에서 approve()를 호출하면") {
            val rental = createRentalWithNullDomainEvents()

            Then("NPE 없이 정상 승인되어야 한다") {
                rental.approve()
                rental.status shouldBe RentalStatus.APPROVED
            }
        }
    }
})
