package com.rental.commerce.infrastructure.refund.mysql

import com.querydsl.jpa.impl.JPAQueryFactory
import com.rental.commerce.domain.refund.QRefund.refund
import com.rental.commerce.domain.refund.Refund
import com.rental.commerce.domain.refund.RefundRepository
import com.rental.commerce.domain.refund.RefundStatus
import org.springframework.stereotype.Repository
import java.math.BigDecimal

/**
 * RefundRepositoryImpl — JPA + QueryDSL 환불 Repository 구현 (ADR-009).
 *
 * sumNonFailedAmountByPaymentId: 누적 환불 금액 계산 (PENDING + SUCCEEDED 만).
 * @Query 금지 규칙 준수를 위해 QueryDSL 사용.
 */
@Repository
class RefundRepositoryImpl(
    private val refundJpaRepository: RefundJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : RefundRepository {

    override fun save(refund: Refund): Refund = refundJpaRepository.save(refund)

    override fun findById(id: Long): Refund? = refundJpaRepository.findById(id).orElse(null)

    override fun sumNonFailedAmountByPaymentId(paymentId: Long): BigDecimal {
        val sum = queryFactory
            .select(refund.amount.sum())
            .from(refund)
            .where(
                refund.paymentId.eq(paymentId),
                refund.status.`in`(RefundStatus.PENDING, RefundStatus.SUCCEEDED),
            )
            .fetchOne()
        return sum ?: BigDecimal.ZERO
    }

    override fun findByDisputeId(disputeId: Long): List<Refund> =
        refundJpaRepository.findAllByDisputeId(disputeId)
}
