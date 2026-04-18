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

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    val netAmount: BigDecimal,

    status: SettlementStatus = SettlementStatus.PENDING,

) : BaseEntity() {

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
