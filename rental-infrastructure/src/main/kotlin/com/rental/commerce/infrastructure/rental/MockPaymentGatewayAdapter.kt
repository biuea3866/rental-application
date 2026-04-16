package com.rental.commerce.infrastructure.rental

import com.rental.commerce.domain.rental.PaymentApproveRequest
import com.rental.commerce.domain.rental.PaymentCancelResult
import com.rental.commerce.domain.rental.PaymentGateway
import com.rental.commerce.domain.rental.PaymentResult
import java.time.ZonedDateTime
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * 로컬/개발/테스트 환경용 Mock PaymentGateway 구현체.
 *
 * @Profile("local", "dev", "test") — 운영/스테이징 외 환경에서만 활성화.
 * 항상 성공 결과를 반환하며 외부 API를 호출하지 않는다.
 */
@Component
@Profile("local", "dev", "test")
class MockPaymentGatewayAdapter : PaymentGateway {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun requestPayment(request: PaymentApproveRequest): PaymentResult {
        val mockPaymentKey = "mock-pay-${UUID.randomUUID()}"
        log.info(
            "[MockPaymentGateway] 결제 승인 성공 처리. orderId={}, amount={}, mockPaymentKey={}",
            request.orderId,
            request.amount,
            mockPaymentKey,
        )
        return PaymentResult(
            success = true,
            paymentKey = mockPaymentKey,
            orderId = request.orderId,
            amount = request.amount,
            approvedAt = ZonedDateTime.now(),
            failureMessage = null,
        )
    }

    override fun cancelPayment(paymentKey: String, reason: String): PaymentCancelResult {
        log.info(
            "[MockPaymentGateway] 결제 취소 성공 처리. paymentKey={}, reason={}",
            paymentKey,
            reason,
        )
        return PaymentCancelResult(
            success = true,
            cancelAmount = 0L,
            canceledAt = ZonedDateTime.now(),
        )
    }
}
