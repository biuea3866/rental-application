package com.rental.commerce.infrastructure.refund.gateway

import com.rental.commerce.domain.refund.port.PaymentRefundGateway
import com.rental.commerce.domain.refund.port.PaymentRefundResult
import java.math.BigDecimal
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * 로컬/개발/테스트 환경용 Mock PaymentRefund Adapter.
 *
 * 항상 성공 결과를 반환하며 외부 API 호출 없음.
 * 테스트에서 실패 케이스를 만들고 싶으면 MockK 로 override.
 */
@Component
@Profile("local", "dev", "test")
class MockPaymentRefundAdapter : PaymentRefundGateway {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun requestRefund(paymentKey: String, amount: BigDecimal, reason: String): PaymentRefundResult {
        val externalRefundId = "mock-refund-${UUID.randomUUID()}"
        log.info(
            "[MockPaymentRefundAdapter] 환불 성공 처리 paymentKey={} amount={} reason={} externalRefundId={}",
            paymentKey, amount, reason, externalRefundId,
        )
        return PaymentRefundResult.succeeded(externalRefundId)
    }
}
