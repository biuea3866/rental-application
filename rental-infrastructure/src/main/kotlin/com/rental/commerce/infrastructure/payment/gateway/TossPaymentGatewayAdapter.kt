package com.rental.commerce.infrastructure.payment.gateway

import com.rental.commerce.domain.common.PaymentFailedException
import com.rental.commerce.domain.rental.PaymentApproveRequest
import com.rental.commerce.domain.rental.PaymentApproveResult
import com.rental.commerce.domain.rental.PaymentCancelResult
import com.rental.commerce.domain.rental.PaymentGateway
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.ZonedDateTime
import java.util.Base64

/**
 * Toss Payments API Adapter.
 * Port: PaymentGateway
 * Adapter: TossPaymentGatewayAdapter
 *
 * prod/staging 환경에서 활성화됩니다.
 */
@Component
@Profile("prod", "staging")
class TossPaymentGatewayAdapter(
    private val properties: TossPaymentProperties,
    private val restClient: RestClient,
) : PaymentGateway {

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun basicAuthHeader(): String {
        val credentials = "${properties.secretKey}:"
        val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray())
        return "Basic $encoded"
    }

    /**
     * Toss Payments 결제 승인 API 호출.
     * POST https://api.tosspayments.com/v1/payments/confirm
     */
    override fun approve(request: PaymentApproveRequest): PaymentApproveResult {
        logger.info("[TossPayments] 결제 승인 요청 - orderId: {}, amount: {}", request.orderId, request.amount)

        return try {
            val response = restClient.post()
                .uri("${properties.baseUrl}/v1/payments/confirm")
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    TossConfirmRequest(
                        paymentKey = request.paymentKey,
                        orderId = request.orderId,
                        amount = request.amount,
                    )
                )
                .retrieve()
                .body(TossPaymentResponse::class.java)
                ?: throw PaymentFailedException("Toss Payments 응답이 비어 있습니다")

            logger.info("[TossPayments] 결제 승인 완료 - paymentKey: {}", response.paymentKey)

            PaymentApproveResult(
                externalPaymentId = response.paymentKey,
                approvedAt = response.approvedAt ?: ZonedDateTime.now(),
                method = response.method ?: "UNKNOWN",
            )
        } catch (e: RestClientException) {
            logger.error("[TossPayments] 결제 승인 실패 - orderId: {}, error: {}", request.orderId, e.message)
            throw PaymentFailedException("Toss Payments 결제 승인에 실패했습니다: ${e.message}")
        }
    }

    /**
     * Toss Payments 결제 취소 API 호출.
     * POST https://api.tosspayments.com/v1/payments/{paymentKey}/cancel
     */
    override fun cancel(paymentKey: String, cancelAmount: Long, reason: String): PaymentCancelResult {
        logger.info("[TossPayments] 결제 취소 요청 - paymentKey: {}, cancelAmount: {}", paymentKey, cancelAmount)

        return try {
            val response = restClient.post()
                .uri("${properties.baseUrl}/v1/payments/$paymentKey/cancel")
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    TossCancelRequest(
                        cancelReason = reason,
                        cancelAmount = cancelAmount,
                    )
                )
                .retrieve()
                .body(TossCancelResponse::class.java)
                ?: throw PaymentFailedException("Toss Payments 취소 응답이 비어 있습니다")

            logger.info("[TossPayments] 결제 취소 완료 - paymentKey: {}", paymentKey)

            PaymentCancelResult(
                paymentKey = paymentKey,
                cancelledAt = response.cancels?.firstOrNull()?.canceledAt ?: ZonedDateTime.now(),
                cancelAmount = cancelAmount,
            )
        } catch (e: RestClientException) {
            logger.error("[TossPayments] 결제 취소 실패 - paymentKey: {}, error: {}", paymentKey, e.message)
            throw PaymentFailedException("Toss Payments 결제 취소에 실패했습니다: ${e.message}")
        }
    }

    // DTO 내부 클래스
    private data class TossConfirmRequest(
        val paymentKey: String,
        val orderId: String,
        val amount: Long,
    )

    private data class TossPaymentResponse(
        val paymentKey: String,
        val orderId: String,
        val status: String?,
        val method: String?,
        val approvedAt: ZonedDateTime?,
        val totalAmount: Long?,
    )

    private data class TossCancelRequest(
        val cancelReason: String,
        val cancelAmount: Long?,
    )

    private data class TossCancelResponse(
        val paymentKey: String?,
        val cancels: List<TossCancelDetail>?,
    )

    private data class TossCancelDetail(
        val cancelAmount: Long?,
        val canceledAt: ZonedDateTime?,
        val cancelReason: String?,
    )
}
