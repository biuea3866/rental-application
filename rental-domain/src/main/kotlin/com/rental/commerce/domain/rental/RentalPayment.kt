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
class RentalPayment private constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L,

    @Column(name = "rental_id", nullable = false)
    val rentalId: Long,

    @Column(name = "amount", nullable = false)
    val amount: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    val paymentMethod: PaymentMethod,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PaymentStatus = PaymentStatus.PENDING,

    @Column(name = "external_payment_id", length = 200)
    var externalPaymentId: String? = null,

    @Column(name = "order_id", length = 200, nullable = false)
    val orderId: String,

    @Column(name = "paid_at")
    var paidAt: ZonedDateTime? = null,

    @Column(name = "refunded_at")
    var refundedAt: ZonedDateTime? = null,

) : BaseEntity() {

    fun complete(externalPaymentId: String) {
        if (!status.canTransitTo(PaymentStatus.COMPLETED)) {
            throw InvalidStateTransitionException(
                "${status.name}에서 COMPLETED(으)로 전이할 수 없습니다"
            )
        }
        this.status = PaymentStatus.COMPLETED
        this.externalPaymentId = externalPaymentId
        this.paidAt = ZonedDateTime.now()
    }

    fun fail() {
        if (!status.canTransitTo(PaymentStatus.FAILED)) {
            throw InvalidStateTransitionException(
                "${status.name}에서 FAILED(으)로 전이할 수 없습니다"
            )
        }
        this.status = PaymentStatus.FAILED
    }

    fun refund() {
        if (!status.canTransitTo(PaymentStatus.REFUNDED)) {
            throw InvalidStateTransitionException(
                "${status.name}에서 REFUNDED(으)로 전이할 수 없습니다"
            )
        }
        this.status = PaymentStatus.REFUNDED
        this.refundedAt = ZonedDateTime.now()
    }

    companion object {
        fun create(
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
                status = PaymentStatus.PENDING,
            )
        }
    }
}
