package com.rental.commerce.domain.settlement

import com.rental.commerce.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZonedDateTime

@Entity
@Table(
    name = "settlement",
    uniqueConstraints = [UniqueConstraint(columnNames = ["rental_id"])],
)
class Settlement private constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L,

    @Column(name = "lender_id", nullable = false)
    val lenderId: Long,

    @Column(name = "rental_id", nullable = false)
    val rentalId: Long,

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    val amount: BigDecimal,

    @Column(name = "commission", nullable = false, precision = 15, scale = 2)
    val commission: BigDecimal,

    netAmount: BigDecimal,

    status: SettlementStatus = SettlementStatus.PENDING,

) : BaseEntity() {

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    var netAmount: BigDecimal = netAmount
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: SettlementStatus = status
        protected set

    @Column(name = "settled_at")
    var settledAt: ZonedDateTime? = null
        protected set

    /**
     * 정산을 완료 처리한다.
     * PENDING 상태에서만 호출 가능하며, 상태 검증은 SettlementStatus 내부에 캡슐화한다.
     */
    fun complete() {
        status.validateCanComplete()
        this.status = SettlementStatus.COMPLETED
        this.settledAt = ZonedDateTime.now()
    }

    /**
     * 환불 총액 기준으로 net_amount 를 재계산한다 (BE-406, idempotent).
     *
     * net_amount = amount - commission - totalRefunded
     * 음수 금지: totalRefunded 가 (amount - commission) 을 넘는 경우 예외.
     *
     * 동일 환불 이벤트 중복 수신 시에도 totalRefunded 가 동일하면 결과가 같으므로 안전.
     */
    fun applyRefundAdjustment(totalRefunded: BigDecimal) {
        require(totalRefunded >= BigDecimal.ZERO) { "환불 총액은 0 이상이어야 합니다" }
        val base = amount.subtract(commission)
        val newNet = base.subtract(totalRefunded).setScale(2, RoundingMode.HALF_UP)
        require(newNet >= BigDecimal.ZERO) {
            "환불 총액이 수수료 제외 정산 기준금액을 초과합니다. base=$base totalRefunded=$totalRefunded"
        }
        this.netAmount = newNet
    }

    companion object {

        /**
         * 정산을 생성한다.
         * 수수료(commission) = amount * commissionRate (소수점 2자리 반올림)
         * 실수령액(netAmount) = amount - commission
         */
        fun create(
            lenderId: Long,
            rentalId: Long,
            amount: BigDecimal,
            commissionRate: BigDecimal,
        ): Settlement {
            val commission = amount.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP)
            val netAmount = amount.subtract(commission).setScale(2, RoundingMode.HALF_UP)
            return Settlement(
                lenderId = lenderId,
                rentalId = rentalId,
                amount = amount.setScale(2, RoundingMode.HALF_UP),
                commission = commission,
                netAmount = netAmount,
            )
        }
    }
}
