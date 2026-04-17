package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
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

    fun createRental(
        renterId: Long = 1L,
        lenderId: Long = 2L,
        productId: Long = 10L,
    ): Rental {
        return Rental.create(
            renterId = renterId,
            lenderId = lenderId,
            productId = productId,
            startDate = ZonedDateTime.now().plusDays(1),
            endDate = ZonedDateTime.now().plusDays(7),
            totalAmount = 70_000L,
            depositAmount = 50_000L,
            deliveryInfo = DeliveryInfo(
                recipientName = "홍길동",
                recipientPhone = "010-1234-5678",
                addressLine1 = "서울특별시 강남구 테헤란로 123",
                addressLine2 = "101호",
                zipCode = "06234",
            ),
        )
    }

    Given("Rental.create() — 신규 대여 생성") {

        When("필수 정보로 create()를 호출하면") {
            val rental = createRental()

            Then("status가 REQUESTED로 초기화된다") {
                rental.status shouldBe RentalStatus.REQUESTED
            }

            Then("renterId, lenderId, productId가 저장된다") {
                rental.renterId shouldBe 1L
                rental.lenderId shouldBe 2L
                rental.productId shouldBe 10L
            }

            Then("requestedAt이 설정된다") {
                rental.requestedAt shouldNotBe null
            }

            Then("version 필드가 존재한다") {
                rental.version shouldBe 0
            }
        }
    }

    Given("approve() — REQUESTED → APPROVED 전이") {

        When("REQUESTED 상태에서 approve()를 호출하면") {
            val rental = createRental()
            rental.approve()

            Then("status가 APPROVED로 변경된다") {
                rental.status shouldBe RentalStatus.APPROVED
            }

            Then("approvedAt이 설정된다") {
                rental.approvedAt shouldNotBe null
            }

            Then("RentalStatusChangedEvent가 발행된다") {
                val events = rental.pullEvents()
                events shouldHaveSize 1
                val event = events.first()
                event.shouldBeInstanceOf<RentalStatusChangedEvent>()
                (event as RentalStatusChangedEvent).toStatus shouldBe RentalStatus.APPROVED
            }
        }

        When("REQUESTED가 아닌 상태(PAID)에서 approve()를 호출하면") {
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            rental.markPaid()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    rental.approve()
                }
            }
        }
    }

    Given("reject() — REQUESTED → CANCELLED 전이") {

        When("REQUESTED 상태에서 reject()를 호출하면") {
            val rental = createRental()
            rental.reject(reason = "대여 불가 기간입니다")

            Then("status가 CANCELLED로 변경된다") {
                rental.status shouldBe RentalStatus.CANCELLED
            }

            Then("cancelReason이 설정된다") {
                rental.cancelReason shouldBe "대여 불가 기간입니다"
            }

            Then("cancelledAt이 설정된다") {
                rental.cancelledAt shouldNotBe null
            }

            Then("RentalStatusChangedEvent(CANCELLED)가 발행된다") {
                val events = rental.pullEvents()
                events shouldHaveSize 1
                val event = events.first()
                event.shouldBeInstanceOf<RentalStatusChangedEvent>()
                (event as RentalStatusChangedEvent).toStatus shouldBe RentalStatus.CANCELLED
            }
        }

        When("REQUESTED가 아닌 상태에서 reject()를 호출하면") {
            val rental = createRental()
            rental.approve()
            rental.pullEvents()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    rental.reject(reason = "거절 시도")
                }
            }
        }
    }

    Given("markPaid() — APPROVED → PAID 전이") {

        When("APPROVED 상태에서 markPaid()를 호출하면") {
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            rental.markPaid()

            Then("status가 PAID로 변경된다") {
                rental.status shouldBe RentalStatus.PAID
            }

            Then("paidAt이 설정된다") {
                rental.paidAt shouldNotBe null
            }

            Then("RentalStatusChangedEvent(PAID)가 발행된다") {
                val events = rental.pullEvents()
                events shouldHaveSize 1
                val event = events.first()
                event.shouldBeInstanceOf<RentalStatusChangedEvent>()
                (event as RentalStatusChangedEvent).fromStatus shouldBe RentalStatus.APPROVED
                (event as RentalStatusChangedEvent).toStatus shouldBe RentalStatus.PAID
            }
        }

        When("APPROVED가 아닌 상태에서 markPaid()를 호출하면") {
            val rental = createRental()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    rental.markPaid()
                }
            }
        }
    }

    Given("startRental() — PAID → IN_USE 전이") {

        When("PAID 상태에서 startRental()을 호출하면") {
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            rental.markPaid()
            rental.startRental()

            Then("status가 IN_USE로 변경된다") {
                rental.status shouldBe RentalStatus.IN_USE
            }

            Then("startedAt이 설정된다") {
                rental.startedAt shouldNotBe null
            }
        }

        When("PAID가 아닌 상태에서 startRental()을 호출하면") {
            val rental = createRental()
            rental.approve()
            rental.pullEvents()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    rental.startRental()
                }
            }
        }
    }

    Given("returnRental() — IN_USE → RETURNED 전이") {

        When("IN_USE 상태에서 returnRental()을 호출하면") {
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            rental.markPaid()
            rental.startRental()
            rental.returnRental()

            Then("status가 RETURNED로 변경된다") {
                rental.status shouldBe RentalStatus.RETURNED
            }

            Then("returnedAt이 설정된다") {
                rental.returnedAt shouldNotBe null
            }
        }

        When("IN_USE가 아닌 상태에서 returnRental()을 호출하면") {
            val rental = createRental()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    rental.returnRental()
                }
            }
        }
    }

    Given("cancel() — REQUESTED/APPROVED/PAID → CANCELLED 전이") {

        When("REQUESTED 상태에서 cancel()을 호출하면") {
            val rental = createRental()
            rental.cancel(reason = "변심으로 인한 취소")

            Then("status가 CANCELLED로 변경된다") {
                rental.status shouldBe RentalStatus.CANCELLED
            }

            Then("cancelReason이 설정된다") {
                rental.cancelReason shouldBe "변심으로 인한 취소"
            }
        }

        When("APPROVED 상태에서 cancel()을 호출하면") {
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            rental.cancel(reason = "등록자 취소")

            Then("status가 CANCELLED로 변경된다") {
                rental.status shouldBe RentalStatus.CANCELLED
            }
        }

        When("PAID 상태에서 cancel()을 호출하면") {
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            rental.markPaid()
            rental.cancel(reason = "결제 후 취소")

            Then("status가 CANCELLED로 변경된다 (환불 처리는 UseCase에서 수행)") {
                rental.status shouldBe RentalStatus.CANCELLED
            }

            Then("cancelReason이 설정된다") {
                rental.cancelReason shouldBe "결제 후 취소"
            }
        }

        When("IN_USE 상태에서 cancel()을 호출하면") {
            val rental = createRental()
            rental.approve()
            rental.markPaid()
            rental.startRental()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    rental.cancel(reason = "IN_USE 상태 취소 시도")
                }
            }
        }
    }

    Given("캡슐화 검증 메서드") {

        When("isOwnedByLender()를 lenderId와 동일한 userId로 호출하면") {
            val rental = createRental(lenderId = 2L)

            Then("true를 반환한다") {
                rental.isOwnedByLender(2L) shouldBe true
            }
        }

        When("isOwnedByLender()를 다른 userId로 호출하면") {
            val rental = createRental(lenderId = 2L)

            Then("false를 반환한다") {
                rental.isOwnedByLender(999L) shouldBe false
            }
        }

        When("isRequestedByRenter()를 renterId와 동일한 userId로 호출하면") {
            val rental = createRental(renterId = 1L)

            Then("true를 반환한다") {
                rental.isRequestedByRenter(1L) shouldBe true
            }
        }

        When("isRequestedByRenter()를 다른 userId로 호출하면") {
            val rental = createRental(renterId = 1L)

            Then("false를 반환한다") {
                rental.isRequestedByRenter(999L) shouldBe false
            }
        }

        When("isParticipant()를 renterId로 호출하면") {
            val rental = createRental(renterId = 1L, lenderId = 2L)

            Then("true를 반환한다") {
                rental.isParticipant(1L) shouldBe true
            }
        }

        When("isParticipant()를 lenderId로 호출하면") {
            val rental = createRental(renterId = 1L, lenderId = 2L)

            Then("true를 반환한다") {
                rental.isParticipant(2L) shouldBe true
            }
        }

        When("isParticipant()를 참여자가 아닌 userId로 호출하면") {
            val rental = createRental(renterId = 1L, lenderId = 2L)

            Then("false를 반환한다") {
                rental.isParticipant(999L) shouldBe false
            }
        }
    }

    Given("verifyLenderAuthority() — 등록자 권한 검증 캡슐화") {

        When("lenderId와 일치하는 userId로 호출하면") {
            val rental = createRental(lenderId = 2L)

            Then("예외 없이 통과한다") {
                rental.verifyLenderAuthority(2L)
            }
        }

        When("lenderId와 다른 userId로 호출하면") {
            val rental = createRental(lenderId = 2L)

            Then("FORBIDDEN BusinessException이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    rental.verifyLenderAuthority(999L)
                }
                exception.errorCode shouldBe ErrorCode.FORBIDDEN
            }
        }
    }

    Given("verifyRenterAuthority() — 대여자 권한 검증 캡슐화") {

        When("renterId와 일치하는 userId로 호출하면") {
            val rental = createRental(renterId = 1L)

            Then("예외 없이 통과한다") {
                rental.verifyRenterAuthority(1L)
            }
        }

        When("renterId와 다른 userId로 호출하면") {
            val rental = createRental(renterId = 1L)

            Then("FORBIDDEN BusinessException이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    rental.verifyRenterAuthority(999L)
                }
                exception.errorCode shouldBe ErrorCode.FORBIDDEN
            }
        }
    }

    Given("verifyParticipant() — 참여자 검증 캡슐화") {

        When("renterId로 호출하면") {
            val rental = createRental(renterId = 1L, lenderId = 2L)

            Then("예외 없이 통과한다") {
                rental.verifyParticipant(1L)
            }
        }

        When("lenderId로 호출하면") {
            val rental = createRental(renterId = 1L, lenderId = 2L)

            Then("예외 없이 통과한다") {
                rental.verifyParticipant(2L)
            }
        }

        When("참여자가 아닌 userId로 호출하면") {
            val rental = createRental(renterId = 1L, lenderId = 2L)

            Then("FORBIDDEN BusinessException이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    rental.verifyParticipant(999L)
                }
                exception.errorCode shouldBe ErrorCode.FORBIDDEN
            }
        }
    }

    Given("isPaid() — PAID 상태 확인") {

        When("PAID 상태인 경우") {
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            rental.markPaid()
            rental.pullEvents()

            Then("true를 반환한다") {
                rental.isPaid() shouldBe true
            }
        }

        When("PAID가 아닌 상태인 경우") {
            val rental = createRental()

            Then("false를 반환한다") {
                rental.isPaid() shouldBe false
            }
        }
    }

    Given("pullEvents() — 이벤트 수집 및 초기화") {

        When("approve() 후 pullEvents()를 두 번 호출하면") {
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            val secondPull = rental.pullEvents()

            Then("두 번째 호출에서는 빈 리스트가 반환된다") {
                secondPull shouldHaveSize 0
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // 엣지 케이스: 상태 전이 순서 및 이벤트 발행 순서
    // ────────────────────────────────────────────────────────────────────────

    Given("이벤트 발행 순서 — 전체 정상 플로우 순차 상태 전이") {

        When("REQUESTED → APPROVED → PAID → IN_USE → RETURNED 순으로 전이하면") {
            val rental = createRental()
            rental.approve()
            val approveEvents = rental.pullEvents()
            rental.markPaid()
            val paidEvents = rental.pullEvents()
            rental.startRental()
            val startEvents = rental.pullEvents()
            rental.returnRental()
            val returnEvents = rental.pullEvents()

            Then("각 단계마다 정확히 1개의 RentalStatusChangedEvent가 발행된다") {
                approveEvents shouldHaveSize 1
                paidEvents shouldHaveSize 1
                startEvents shouldHaveSize 1
                returnEvents shouldHaveSize 1
            }

            Then("각 이벤트의 fromStatus, toStatus가 전이 순서와 일치한다") {
                val approveEvent = approveEvents.first().also { it.shouldBeInstanceOf<RentalStatusChangedEvent>() } as RentalStatusChangedEvent
                approveEvent.fromStatus shouldBe RentalStatus.REQUESTED
                approveEvent.toStatus shouldBe RentalStatus.APPROVED

                val paidEvent = paidEvents.first().also { it.shouldBeInstanceOf<RentalStatusChangedEvent>() } as RentalStatusChangedEvent
                paidEvent.fromStatus shouldBe RentalStatus.APPROVED
                paidEvent.toStatus shouldBe RentalStatus.PAID

                val startEvent = startEvents.first().also { it.shouldBeInstanceOf<RentalStatusChangedEvent>() } as RentalStatusChangedEvent
                startEvent.fromStatus shouldBe RentalStatus.PAID
                startEvent.toStatus shouldBe RentalStatus.IN_USE

                val returnEvent = returnEvents.first().also { it.shouldBeInstanceOf<RentalStatusChangedEvent>() } as RentalStatusChangedEvent
                returnEvent.fromStatus shouldBe RentalStatus.IN_USE
                returnEvent.toStatus shouldBe RentalStatus.RETURNED
            }

            Then("최종 status는 RETURNED이다") {
                rental.status shouldBe RentalStatus.RETURNED
            }
        }
    }

    Given("취소 후 재전이 불가 — CANCELLED는 terminal state") {

        When("REQUESTED 상태에서 cancel() 후 approve()를 시도하면") {
            val rental = createRental()
            rental.cancel(reason = "취소 후 재승인 시도")
            rental.pullEvents()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    rental.approve()
                }
            }
        }

        When("APPROVED 상태에서 cancel() 후 markPaid()를 시도하면") {
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            rental.cancel(reason = "취소 후 결제 시도")
            rental.pullEvents()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    rental.markPaid()
                }
            }
        }

        When("PAID 상태에서 cancel() 후 startRental()을 시도하면") {
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            rental.markPaid()
            rental.pullEvents()
            rental.cancel(reason = "결제 후 취소 후 시작 시도")
            rental.pullEvents()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    rental.startRental()
                }
            }
        }

        When("CANCELLED 상태에서 cancel()을 다시 호출하면") {
            val rental = createRental()
            rental.cancel(reason = "첫 취소")
            rental.pullEvents()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    rental.cancel(reason = "중복 취소 시도")
                }
            }
        }
    }

    Given("RETURNED 상태 — terminal state 전이 불가") {

        When("RETURNED 상태에서 cancel()을 시도하면") {
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            rental.markPaid()
            rental.pullEvents()
            rental.startRental()
            rental.pullEvents()
            rental.returnRental()
            rental.pullEvents()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    rental.cancel(reason = "반납 후 취소 시도")
                }
            }
        }

        When("RETURNED 상태에서 approve()를 시도하면") {
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            rental.markPaid()
            rental.pullEvents()
            rental.startRental()
            rental.pullEvents()
            rental.returnRental()
            rental.pullEvents()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    rental.approve()
                }
            }
        }
    }

    Given("reject() 후 재전이 불가 — reject()는 CANCELLED terminal state") {

        When("reject() 후 approve()를 시도하면") {
            val rental = createRental()
            rental.reject(reason = "대여 불가")
            rental.pullEvents()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    rental.approve()
                }
            }
        }

        When("reject() 후 markPaid()를 시도하면") {
            val rental = createRental()
            rental.reject(reason = "대여 불가")
            rental.pullEvents()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    rental.markPaid()
                }
            }
        }
    }

    Given("이벤트 발행 — renterId, lenderId가 이벤트에 정확히 포함된다") {

        When("renterId=1, lenderId=2로 생성한 대여에서 approve()를 호출하면") {
            val rental = createRental(renterId = 1L, lenderId = 2L)
            rental.approve()
            val events = rental.pullEvents()

            Then("이벤트의 renterId=1, lenderId=2이다") {
                val event = events.first().also { it.shouldBeInstanceOf<RentalStatusChangedEvent>() } as RentalStatusChangedEvent
                event.renterId shouldBe 1L
                event.lenderId shouldBe 2L
            }
        }
    }

    Given("cancel() — cancel 사유가 정확히 저장된다") {

        When("REQUESTED 상태에서 빈 문자열 사유로 cancel()을 호출하면") {
            val rental = createRental()
            rental.cancel(reason = "")

            Then("cancelReason이 빈 문자열로 저장된다") {
                rental.cancelReason shouldBe ""
            }
        }

        When("REQUESTED 상태에서 최대 길이(500자) 사유로 cancel()을 호출하면") {
            val rental = createRental()
            val longReason = "취소".repeat(250)
            rental.cancel(reason = longReason)

            Then("cancelReason이 해당 문자열로 저장된다") {
                rental.cancelReason shouldBe longReason
            }
        }
    }

    Given("isPaid() — 모든 비-PAID 상태에서 false를 반환한다") {

        When("REQUESTED 상태인 경우") {
            val rental = createRental()
            Then("false를 반환한다") { rental.isPaid() shouldBe false }
        }

        When("APPROVED 상태인 경우") {
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            Then("false를 반환한다") { rental.isPaid() shouldBe false }
        }

        When("IN_USE 상태인 경우") {
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            rental.markPaid()
            rental.pullEvents()
            rental.startRental()
            rental.pullEvents()
            Then("false를 반환한다") { rental.isPaid() shouldBe false }
        }

        When("RETURNED 상태인 경우") {
            val rental = createRental()
            rental.approve()
            rental.pullEvents()
            rental.markPaid()
            rental.pullEvents()
            rental.startRental()
            rental.pullEvents()
            rental.returnRental()
            rental.pullEvents()
            Then("false를 반환한다") { rental.isPaid() shouldBe false }
        }

        When("CANCELLED 상태인 경우") {
            val rental = createRental()
            rental.cancel(reason = "취소")
            rental.pullEvents()
            Then("false를 반환한다") { rental.isPaid() shouldBe false }
        }
    }
})
