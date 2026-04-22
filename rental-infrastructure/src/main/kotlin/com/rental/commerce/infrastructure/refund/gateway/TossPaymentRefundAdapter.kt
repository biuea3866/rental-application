package com.rental.commerce.infrastructure.refund.gateway

import com.rental.commerce.domain.refund.port.PaymentRefundGateway
import com.rental.commerce.domain.refund.port.PaymentRefundResult
import com.rental.commerce.infrastructure.rental.TossPaymentProperties
import java.math.BigDecimal
import java.util.Base64
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Toss Payments 환불 API Adapter (Port: PaymentRefundGateway).
 *
 * @Profile("prod", "staging") — 운영/스테이징에서만 활성화.
 * 로컬/테스트는 FakePaymentRefundAdapter 사용.
 */
@Component
@Profile("prod", "staging")
class TossPaymentRefundAdapter(
    private val properties: TossPaymentProperties,
) : PaymentRefundGateway {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient: RestClient by lazy {
        RestClient.builder()
            .baseUrl(properties.baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.AUTHORIZATION, buildAuthHeader())
            .build()
    }

    override fun requestRefund(paymentKey: String, amount: BigDecimal, reason: String): PaymentRefundResult {
        return try {
            val body = mapOf(
                "cancelReason" to reason,
                "cancelAmount" to amount.toPlainString(),
            )
            val response = restClient
                .post()
                .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                .body(body)
                .retrieve()
                .body(TossRefundResponse::class.java)

            log.info(
                "[TossPaymentRefundAdapter] 환불 완료 paymentKey={} amount={} transactionKey={}",
                paymentKey, amount, response?.transactionKey,
            )
            PaymentRefundResult.succeeded(
                externalRefundId = response?.transactionKey
                    ?: "toss-unknown-${System.currentTimeMillis()}",
            )
        } catch (ex: Exception) {
            log.error("[TossPaymentRefundAdapter] 환불 실패 paymentKey={} error={}", paymentKey, ex.message, ex)
            PaymentRefundResult.failed(ex.message ?: "Toss API error")
        }
    }

    private fun buildAuthHeader(): String {
        val token = Base64.getEncoder().encodeToString("${properties.secretKey}:".toByteArray())
        return "Basic $token"
    }

    private data class TossRefundResponse(
        val transactionKey: String?,
        val status: String?,
    )
}
