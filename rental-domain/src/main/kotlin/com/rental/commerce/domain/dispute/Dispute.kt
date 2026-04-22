package com.rental.commerce.domain.dispute

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
 * Dispute Aggregate Root (ADR-009).
 *
 * 상태 전이:
 *   OPEN ─ startReview() ─▶ UNDER_REVIEW ─ resolveFullRefund/Partial/Rejected() ─▶ RESOLVED_*
 *   OPEN ─ cancel(opener) ─▶ CANCELLED
 *
 * 활성 분쟁 단일 제약은 DB generated column + UNIQUE 로 보장(V18).
 */
@Entity
@Table(name = "dispute")
class Dispute private constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L,

    @Column(name = "rental_id", nullable = false)
    val rentalId: Long,

    @Column(name = "opener_id", nullable = false)
    val openerId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    val reason: DisputeReason,

    @Column(name = "description", nullable = false, length = 1000)
    var description: String,

    status: DisputeStatus = DisputeStatus.OPEN,

) : BaseEntity() {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: DisputeStatus = status
        protected set

    @Column(name = "refund_amount", precision = 15, scale = 2)
    var refundAmount: BigDecimal? = null
        protected set

    @Column(name = "resolved_at")
    var resolvedAt: ZonedDateTime? = null
        protected set

    init {
        require(description.isNotBlank()) { "분쟁 설명은 비어있을 수 없습니다" }
        require(description.length <= 1000) { "분쟁 설명은 1000자를 초과할 수 없습니다" }
    }

    fun startReview() {
        status.validateCanStartReview()
        this.status = DisputeStatus.UNDER_REVIEW
    }

    fun resolveFullRefund(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) { "환불 금액은 0보다 커야 합니다" }
        status.validateCanResolve()
        this.status = DisputeStatus.RESOLVED_REFUND
        this.refundAmount = amount
        this.resolvedAt = ZonedDateTime.now()
    }

    fun resolvePartial(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) { "부분 환불 금액은 0보다 커야 합니다" }
        status.validateCanResolve()
        this.status = DisputeStatus.RESOLVED_PARTIAL
        this.refundAmount = amount
        this.resolvedAt = ZonedDateTime.now()
    }

    fun resolveRejected() {
        status.validateCanResolve()
        this.status = DisputeStatus.RESOLVED_REJECTED
        this.refundAmount = null
        this.resolvedAt = ZonedDateTime.now()
    }

    fun cancel(byUserId: Long) {
        check(byUserId == openerId) { "분쟁 취소는 오픈한 당사자만 가능합니다" }
        status.validateCanCancel()
        this.status = DisputeStatus.CANCELLED
        this.resolvedAt = ZonedDateTime.now()
    }

    companion object {
        fun create(
            rentalId: Long,
            openerId: Long,
            reason: DisputeReason,
            description: String,
        ): Dispute = Dispute(
            rentalId = rentalId,
            openerId = openerId,
            reason = reason,
            description = description,
        )
    }
}
