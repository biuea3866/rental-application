package com.rental.commerce.domain.refund

import com.rental.commerce.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.ZonedDateTime

/**
 * Refund Aggregate Root (ADR-009).
 *
 * - 원 결제(payment_id)의 자식 레코드. FK 없음(DB 규칙).
 * - 누적 금액 검증은 RefundDomainService 에서 SELECT ... FOR UPDATE 로 수행.
 * - 상태: PENDING → SUCCEEDED / FAILED (단방향).
 */
@Entity
@Table(name = "refund")
class Refund private constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L,

    @Column(name = "payment_id", nullable = false)
    val paymentId: Long,

    @Column(name = "rental_id", nullable = false)
    val rentalId: Long,

    @Column(name = "dispute_id")
    val disputeId: Long? = null,

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    val amount: BigDecimal,

    @Column(name = "reason", nullable = false, length = 100)
    val reason: String,

    status: RefundStatus = RefundStatus.PENDING,

) : BaseEntity() {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: RefundStatus = status
        protected set

    @Column(name = "external_refund_id", length = 100)
    var externalRefundId: String? = null
        protected set

    @Column(name = "failure_reason", length = 500)
    var failureReason: String? = null
        protected set

    @Column(name = "processed_at")
    var processedAt: ZonedDateTime? = null
        protected set

    init {
        require(amount > BigDecimal.ZERO) { "환불 금액은 0보다 커야 합니다" }
        require(reason.isNotBlank()) { "환불 사유는 비어있을 수 없습니다" }
    }

    fun markSucceeded(externalRefundId: String) {
        status.validateCanMarkSucceeded()
        this.status = RefundStatus.SUCCEEDED
        this.externalRefundId = externalRefundId
        this.processedAt = ZonedDateTime.now()
    }

    fun markFailed(failureReason: String) {
        status.validateCanMarkFailed()
        this.status = RefundStatus.FAILED
        this.failureReason = failureReason
        this.processedAt = ZonedDateTime.now()
    }

    companion object {
        fun create(
            paymentId: Long,
            rentalId: Long,
            disputeId: Long?,
            amount: BigDecimal,
            reason: String,
        ): Refund = Refund(
            paymentId = paymentId,
            rentalId = rentalId,
            disputeId = disputeId,
            amount = amount,
            reason = reason,
        )
    }
}
