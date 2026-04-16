package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.BaseEntity
import com.rental.commerce.domain.common.InvalidStateTransitionException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "rental_payment")
class RentalPayment(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    val paymentId: Long = 0L,

    @Column(name = "rental_id", nullable = false, unique = true)
    val rentalId: Long,

    @Column(name = "amount", nullable = false)
    val amount: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    val paymentMethod: PaymentMethod,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var paymentStatus: PaymentStatus = PaymentStatus.PENDING,

    @Column(name = "external_payment_id", length = 200, unique = true)
    var externalPaymentId: String? = null,

    @Column(name = "order_id", length = 100, nullable = false, unique = true)
    val orderId: String,

    @Column(name = "paid_at")
    var paidAt: ZonedDateTime? = null,

    @Column(name = "refunded_at")
    var refundedAt: ZonedDateTime? = null,

) : BaseEntity() {

    /**
     * 결제 완료 처리 (PENDING → COMPLETED)
     */
    fun complete(externalId: String) {
        check(paymentStatus == PaymentStatus.PENDING) {
            throw InvalidStateTransitionException(
                "complete() 호출 불가: 현재 상태=$paymentStatus (PENDING 상태에서만 가능)",
            )
        }
        paymentStatus = PaymentStatus.COMPLETED
        externalPaymentId = externalId
        paidAt = ZonedDateTime.now()
    }

    /**
     * 결제 실패 처리 (PENDING → FAILED)
     */
    fun fail() {
        check(paymentStatus == PaymentStatus.PENDING) {
            throw InvalidStateTransitionException(
                "fail() 호출 불가: 현재 상태=$paymentStatus (PENDING 상태에서만 가능)",
            )
        }
        paymentStatus = PaymentStatus.FAILED
    }

    /**
     * 환불 처리 (COMPLETED → REFUNDED)
     */
    fun refund() {
        check(paymentStatus == PaymentStatus.COMPLETED) {
            throw InvalidStateTransitionException(
                "refund() 호출 불가: 현재 상태=$paymentStatus (COMPLETED 상태에서만 가능)",
            )
        }
        paymentStatus = PaymentStatus.REFUNDED
        refundedAt = ZonedDateTime.now()
    }

    companion object {
        fun pending(
            rentalId: Long,
            amount: Long,
            paymentMethod: PaymentMethod,
            orderId: String,
        ): RentalPayment {
            return RentalPayment(
                rentalId = rentalId,
                amount = amount,
                paymentMethod = paymentMethod,
                orderId = orderId,
                paymentStatus = PaymentStatus.PENDING,
            )
        }
    }
}
