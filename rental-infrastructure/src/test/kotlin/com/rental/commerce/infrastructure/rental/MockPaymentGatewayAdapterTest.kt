package com.rental.commerce.infrastructure.rental

import com.rental.commerce.domain.rental.PaymentApproveRequest
import com.rental.commerce.domain.rental.PaymentGateway
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith

class MockPaymentGatewayAdapterTest : BehaviorSpec({

    val gateway: PaymentGateway = MockPaymentGatewayAdapter()

    given("requestPayment 호출 시") {

        `when`("유효한 결제 승인 요청을 전달하면") {
            val request = PaymentApproveRequest(
                orderId = "RC-1001-1713063600000",
                amount = 120_000L,
                paymentKey = "test-payment-key-abc123",
            )
            val result = gateway.requestPayment(request)

            then("성공(success=true) 결과를 반환한다") {
                result.success shouldBe true
            }

            then("paymentKey는 'mock-pay-' 프리픽스를 포함한다") {
                result.paymentKey shouldStartWith "mock-pay-"
            }

            then("orderId가 요청과 동일하다") {
                result.orderId shouldBe request.orderId
            }

            then("amount가 요청과 동일하다") {
                result.amount shouldBe request.amount
            }

            then("approvedAt이 null이 아니다") {
                result.approvedAt shouldNotBe null
            }

            then("failureMessage는 null이다") {
                result.failureMessage shouldBe null
            }
        }
    }

    given("cancelPayment 호출 시") {

        `when`("paymentKey와 reason을 전달하면") {
            val paymentKey = "mock-pay-550e8400-e29b-41d4-a716-446655440000"
            val reason = "고객 요청 취소"
            val result = gateway.cancelPayment(
                paymentKey = paymentKey,
                reason = reason,
            )

            then("성공(success=true) 결과를 반환한다") {
                result.success shouldBe true
            }

            then("cancelAmount가 0보다 크거나 같다") {
                result.cancelAmount shouldBe 0L
            }

            then("canceledAt이 null이 아니다") {
                result.canceledAt shouldNotBe null
            }
        }
    }

    given("requestPayment를 두 번 호출하면") {

        `when`("각각 다른 paymentKey가 생성되는지 확인") {
            val request = PaymentApproveRequest(
                orderId = "RC-1002-1713063600000",
                amount = 50_000L,
                paymentKey = "key-for-idempotency-test",
            )
            val result1 = gateway.requestPayment(request)
            val result2 = gateway.requestPayment(request)

            then("두 결과의 paymentKey가 서로 다르다 (UUID 기반 고유값)") {
                result1.paymentKey shouldNotBe result2.paymentKey
            }
        }
    }
})
