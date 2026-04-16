package com.rental.commerce.infrastructure.payment.gateway

import com.rental.commerce.domain.rental.PaymentApproveRequest
import com.rental.commerce.domain.rental.PaymentApproveResult
import com.rental.commerce.domain.rental.PaymentCancelResult
import com.rental.commerce.domain.rental.PaymentGateway
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.util.UUID

/**
 * MockPaymentGatewayAdapter — dev/local/test 환경에서 항상 결제 성공을 반환합니다.
 */
@Component
@Profile("local", "dev", "test")
class MockPaymentGatewayAdapter : PaymentGateway {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun approve(request: PaymentApproveRequest): PaymentApproveResult {
        val mockExternalId = "mock-${UUID.randomUUID()}"
        logger.info(
            "[MockPayment] 결제 승인 성공 (Mock) - orderId: {}, amount: {}, externalId: {}",
            request.orderId,
            request.amount,
            mockExternalId,
        )
        return PaymentApproveResult(
            externalPaymentId = mockExternalId,
            approvedAt = ZonedDateTime.now(),
            method = "CARD",
        )
    }

    override fun cancel(paymentKey: String, cancelAmount: Long, reason: String): PaymentCancelResult {
        logger.info(
            "[MockPayment] 결제 취소 성공 (Mock) - paymentKey: {}, cancelAmount: {}",
            paymentKey,
            cancelAmount,
        )
        return PaymentCancelResult(
            paymentKey = paymentKey,
            cancelledAt = ZonedDateTime.now(),
            cancelAmount = cancelAmount,
        )
    }
}
