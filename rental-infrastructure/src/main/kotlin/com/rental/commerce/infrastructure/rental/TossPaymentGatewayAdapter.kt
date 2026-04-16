package com.rental.commerce.infrastructure.rental

import com.rental.commerce.domain.rental.PaymentApproveRequest
import com.rental.commerce.domain.rental.PaymentCancelResult
import com.rental.commerce.domain.rental.PaymentGateway
import com.rental.commerce.domain.rental.PaymentResult
import java.time.ZonedDateTime
import java.util.Base64
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Toss Payments REST API를 호출하는 PaymentGateway 구현체.
 *
 * @Profile("prod", "staging") — 운영/스테이징 환경에서만 활성화.
 * 로컬/개발/테스트 환경에서는 MockPaymentGatewayAdapter가 활성화된다.
 */
@Component
@Profile("prod", "staging")
class TossPaymentGatewayAdapter(
    private val properties: TossPaymentProperties,
) : PaymentGateway {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient: RestClient by lazy {
        RestClient.builder()
            .baseUrl(properties.baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.AUTHORIZATION, buildAuthHeader())
            .build()
    }

    override fun requestPayment(request: PaymentApproveRequest): PaymentResult {
        return try {
            val body = mapOf(
                "paymentKey" to request.paymentKey,
                "orderId" to request.orderId,
                "amount" to request.amount,
            )

            val response = restClient.post()
                .uri(properties.confirmPath)
                .body(body)
                .retrieve()
                .body(TossPaymentConfirmResponse::class.java)

            if (response == null) {
                log.warn("Toss Payments 응답이 null입니다. orderId={}", request.orderId)
                return PaymentResult(
                    success = false,
                    paymentKey = request.paymentKey,
                    orderId = request.orderId,
                    amount = request.amount,
                    approvedAt = null,
                    failureMessage = "결제 응답이 비어 있습니다.",
                )
            }

            PaymentResult(
                success = true,
                paymentKey = response.paymentKey,
                orderId = response.orderId,
                amount = response.totalAmount,
                approvedAt = response.approvedAt,
                failureMessage = null,
            )
        } catch (ex: Exception) {
            log.error("Toss Payments 결제 승인 실패. orderId={}, error={}", request.orderId, ex.message)
            PaymentResult(
                success = false,
                paymentKey = request.paymentKey,
                orderId = request.orderId,
                amount = request.amount,
                approvedAt = null,
                failureMessage = ex.message ?: "결제 처리 중 오류가 발생했습니다.",
            )
        }
    }

    override fun cancelPayment(paymentKey: String, reason: String): PaymentCancelResult {
        return try {
            val uri = properties.cancelPath.replace("{paymentKey}", paymentKey)
            val body = mapOf("cancelReason" to reason)

            val response = restClient.post()
                .uri(uri)
                .body(body)
                .retrieve()
                .body(TossPaymentCancelResponse::class.java)

            if (response == null) {
                log.warn("Toss Payments 취소 응답이 null입니다. paymentKey={}", paymentKey)
                return PaymentCancelResult(
                    success = false,
                    cancelAmount = 0L,
                    canceledAt = null,
                )
            }

            val latestCancel = response.cancels.maxByOrNull { it.canceledAt }

            PaymentCancelResult(
                success = true,
                cancelAmount = latestCancel?.cancelAmount ?: 0L,
                canceledAt = latestCancel?.canceledAt,
            )
        } catch (ex: Exception) {
            log.error("Toss Payments 결제 취소 실패. paymentKey={}, error={}", paymentKey, ex.message)
            PaymentCancelResult(
                success = false,
                cancelAmount = 0L,
                canceledAt = null,
            )
        }
    }

    private fun buildAuthHeader(): String {
        val encoded = Base64.getEncoder().encodeToString("${properties.secretKey}:".toByteArray())
        return "Basic $encoded"
    }
}

// ---- 내부 DTO (Toss Payments API 응답 매핑) ----

private data class TossPaymentConfirmResponse(
    val paymentKey: String,
    val orderId: String,
    val totalAmount: Long,
    val approvedAt: ZonedDateTime?,
)

private data class TossPaymentCancelResponse(
    val paymentKey: String,
    val cancels: List<TossCancelDetail> = emptyList(),
)

private data class TossCancelDetail(
    val cancelAmount: Long,
    val canceledAt: ZonedDateTime,
)
