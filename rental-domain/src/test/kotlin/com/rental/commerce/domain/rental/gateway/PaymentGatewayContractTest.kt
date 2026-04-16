package com.rental.commerce.domain.rental.gateway

import com.rental.commerce.domain.rental.PaymentApproveRequest
import com.rental.commerce.domain.rental.PaymentCancelResult
import com.rental.commerce.domain.rental.PaymentGateway
import com.rental.commerce.domain.rental.PaymentResult
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.ZonedDateTime

/**
 * PaymentGateway Port 인터페이스 계약 테스트.
 * 구현체와 무관하게 Port 계약을 검증하는 Fake 구현으로 테스트한다.
 */
class PaymentGatewayContractTest : BehaviorSpec({

    // Fake 구현체 — 계약 테스트용
    val fakeGateway: PaymentGateway = object : PaymentGateway {

        override fun requestPayment(request: PaymentApproveRequest): PaymentResult {
            return PaymentResult(
                success = true,
                paymentKey = "fake-key-${request.orderId}",
                orderId = request.orderId,
                amount = request.amount,
                approvedAt = ZonedDateTime.now(),
                failureMessage = null,
            )
        }

        override fun cancelPayment(paymentKey: String, reason: String): PaymentCancelResult {
            return PaymentCancelResult(
                success = true,
                cancelAmount = 0L,
                canceledAt = ZonedDateTime.now(),
            )
        }
    }

    given("PaymentGateway 계약") {

        `when`("requestPayment 호출 시 성공 케이스") {
            val request = PaymentApproveRequest(
                orderId = "RC-CONTRACT-001",
                amount = 100_000L,
                paymentKey = "contract-test-key",
            )
            val result = fakeGateway.requestPayment(request)

            then("PaymentResult에는 orderId, amount, paymentKey가 포함되어야 한다") {
                result.orderId shouldBe request.orderId
                result.amount shouldBe request.amount
                result.paymentKey shouldNotBe null
            }

            then("성공 시 success=true, failureMessage=null") {
                result.success shouldBe true
                result.failureMessage shouldBe null
            }

            then("approvedAt은 ZonedDateTime 타입이다") {
                result.approvedAt shouldNotBe null
            }
        }

        `when`("requestPayment 실패 케이스") {
            val failGateway: PaymentGateway = object : PaymentGateway {
                override fun requestPayment(request: PaymentApproveRequest): PaymentResult =
                    PaymentResult(
                        success = false,
                        paymentKey = "",
                        orderId = request.orderId,
                        amount = request.amount,
                        approvedAt = null,
                        failureMessage = "카드 한도 초과",
                    )

                override fun cancelPayment(paymentKey: String, reason: String): PaymentCancelResult =
                    PaymentCancelResult(
                        success = false,
                        cancelAmount = 0L,
                        canceledAt = null,
                    )
            }

            val result = failGateway.requestPayment(
                PaymentApproveRequest(
                    orderId = "RC-CONTRACT-FAIL",
                    amount = 9_999_999L,
                    paymentKey = "fail-key",
                )
            )

            then("실패 시 success=false이고 failureMessage가 존재한다") {
                result.success shouldBe false
                result.failureMessage shouldNotBe null
            }

            then("실패 시 approvedAt은 null이다") {
                result.approvedAt shouldBe null
            }
        }

        `when`("cancelPayment 계약 검증") {
            val result = fakeGateway.cancelPayment(
                paymentKey = "some-key",
                reason = "테스트 취소",
            )

            then("PaymentCancelResult에는 success, cancelAmount, canceledAt이 포함된다") {
                result.success shouldBe true
                result.cancelAmount shouldBe 0L
                result.canceledAt shouldNotBe null
            }
        }
    }
})
