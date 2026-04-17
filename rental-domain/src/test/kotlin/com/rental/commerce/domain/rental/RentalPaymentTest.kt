package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.InvalidStateTransitionException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class RentalPaymentTest : BehaviorSpec({

    fun createPendingPayment(
        rentalId: Long = 1L,
        amount: Long = 70_000L,
        paymentMethod: PaymentMethod = PaymentMethod.CARD,
        orderId: String = "RC-0001-1713063600000",
    ): RentalPayment = RentalPayment.create(
        rentalId = rentalId,
        amount = amount,
        paymentMethod = paymentMethod,
        orderId = orderId,
    )

    Given("RentalPayment.create() — 신규 결제 생성") {

        When("필수 정보로 create()를 호출하면") {
            val payment = createPendingPayment()

            Then("status가 PENDING으로 초기화된다") {
                payment.status shouldBe PaymentStatus.PENDING
            }

            Then("rentalId, amount, paymentMethod, orderId가 저장된다") {
                payment.rentalId shouldBe 1L
                payment.amount shouldBe 70_000L
                payment.paymentMethod shouldBe PaymentMethod.CARD
                payment.orderId shouldBe "RC-0001-1713063600000"
            }

            Then("externalPaymentId가 null이다") {
                payment.externalPaymentId shouldBe null
            }

            Then("paidAt이 null이다") {
                payment.paidAt shouldBe null
            }

            Then("refundedAt이 null이다") {
                payment.refundedAt shouldBe null
            }
        }
    }

    Given("complete() — PENDING → COMPLETED 전이") {

        When("PENDING 상태에서 complete()를 호출하면") {
            val payment = createPendingPayment()
            val externalPaymentId = "ext-payment-abc-123"
            payment.complete(externalPaymentId)

            Then("status가 COMPLETED로 변경된다") {
                payment.status shouldBe PaymentStatus.COMPLETED
            }

            Then("externalPaymentId가 설정된다") {
                payment.externalPaymentId shouldBe externalPaymentId
            }

            Then("paidAt이 설정된다") {
                payment.paidAt shouldNotBe null
            }
        }

        When("COMPLETED 상태에서 complete()를 다시 호출하면") {
            val payment = createPendingPayment()
            payment.complete("ext-1")

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    payment.complete("ext-2")
                }
            }
        }

        When("FAILED 상태에서 complete()를 호출하면") {
            val payment = createPendingPayment()
            payment.fail()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    payment.complete("ext-1")
                }
            }
        }
    }

    Given("fail() — PENDING → FAILED 전이") {

        When("PENDING 상태에서 fail()을 호출하면") {
            val payment = createPendingPayment()
            payment.fail()

            Then("status가 FAILED로 변경된다") {
                payment.status shouldBe PaymentStatus.FAILED
            }
        }

        When("COMPLETED 상태에서 fail()을 호출하면") {
            val payment = createPendingPayment()
            payment.complete("ext-1")

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    payment.fail()
                }
            }
        }

        When("FAILED 상태에서 fail()을 다시 호출하면") {
            val payment = createPendingPayment()
            payment.fail()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    payment.fail()
                }
            }
        }
    }

    Given("refund() — COMPLETED → REFUNDED 전이") {

        When("COMPLETED 상태에서 refund()를 호출하면") {
            val payment = createPendingPayment()
            payment.complete("ext-payment-1")
            payment.refund()

            Then("status가 REFUNDED로 변경된다") {
                payment.status shouldBe PaymentStatus.REFUNDED
            }

            Then("refundedAt이 설정된다") {
                payment.refundedAt shouldNotBe null
            }
        }

        When("PENDING 상태에서 refund()를 호출하면") {
            val payment = createPendingPayment()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    payment.refund()
                }
            }
        }

        When("FAILED 상태에서 refund()를 호출하면") {
            val payment = createPendingPayment()
            payment.fail()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    payment.refund()
                }
            }
        }

        When("REFUNDED 상태에서 refund()를 다시 호출하면") {
            val payment = createPendingPayment()
            payment.complete("ext-1")
            payment.refund()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    payment.refund()
                }
            }
        }
    }

    Given("PaymentStatus.canTransitTo() — 전이 허용/차단 규칙 전체 검증") {

        When("PENDING 상태에서 허용 전이를 확인하면") {
            Then("COMPLETED, FAILED로의 전이만 허용된다") {
                PaymentStatus.PENDING.canTransitTo(PaymentStatus.COMPLETED) shouldBe true
                PaymentStatus.PENDING.canTransitTo(PaymentStatus.FAILED) shouldBe true
                PaymentStatus.PENDING.canTransitTo(PaymentStatus.REFUNDED) shouldBe false
                PaymentStatus.PENDING.canTransitTo(PaymentStatus.PENDING) shouldBe false
            }
        }

        When("COMPLETED 상태에서 허용 전이를 확인하면") {
            Then("REFUNDED로의 전이만 허용된다") {
                PaymentStatus.COMPLETED.canTransitTo(PaymentStatus.REFUNDED) shouldBe true
                PaymentStatus.COMPLETED.canTransitTo(PaymentStatus.PENDING) shouldBe false
                PaymentStatus.COMPLETED.canTransitTo(PaymentStatus.FAILED) shouldBe false
                PaymentStatus.COMPLETED.canTransitTo(PaymentStatus.COMPLETED) shouldBe false
            }
        }

        When("FAILED 상태에서 확인하면") {
            Then("어떤 상태로도 전이할 수 없다 (terminal state)") {
                PaymentStatus.FAILED.canTransitTo(PaymentStatus.PENDING) shouldBe false
                PaymentStatus.FAILED.canTransitTo(PaymentStatus.COMPLETED) shouldBe false
                PaymentStatus.FAILED.canTransitTo(PaymentStatus.REFUNDED) shouldBe false
            }
        }

        When("REFUNDED 상태에서 확인하면") {
            Then("어떤 상태로도 전이할 수 없다 (terminal state)") {
                PaymentStatus.REFUNDED.canTransitTo(PaymentStatus.PENDING) shouldBe false
                PaymentStatus.REFUNDED.canTransitTo(PaymentStatus.COMPLETED) shouldBe false
                PaymentStatus.REFUNDED.canTransitTo(PaymentStatus.FAILED) shouldBe false
                PaymentStatus.REFUNDED.canTransitTo(PaymentStatus.REFUNDED) shouldBe false
            }
        }
    }

    Given("금액 엣지 케이스 — 0원 결제") {

        When("amount가 0원인 결제를 생성하면") {
            val payment = createPendingPayment(amount = 0L)

            Then("amount가 0으로 저장된다") {
                payment.amount shouldBe 0L
            }

            Then("complete() 호출 후 COMPLETED로 전이된다") {
                payment.complete("ext-zero-payment")
                payment.status shouldBe PaymentStatus.COMPLETED
            }
        }
    }

    Given("결제 방법 KAKAO_PAY — 정상 생성 및 완료") {

        When("KAKAO_PAY 방법으로 결제를 생성하면") {
            val payment = createPendingPayment(paymentMethod = PaymentMethod.KAKAO_PAY)

            Then("paymentMethod가 KAKAO_PAY로 저장된다") {
                payment.paymentMethod shouldBe PaymentMethod.KAKAO_PAY
            }

            Then("complete() 후 COMPLETED로 전이된다") {
                payment.complete("kakao-ext-key-001")
                payment.status shouldBe PaymentStatus.COMPLETED
                payment.externalPaymentId shouldBe "kakao-ext-key-001"
            }
        }
    }
})
