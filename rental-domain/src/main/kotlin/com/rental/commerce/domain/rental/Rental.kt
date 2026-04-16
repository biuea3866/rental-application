package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.BaseEntity
import com.rental.commerce.domain.common.DomainEvent
import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.rental.event.RentalStatusChangedEvent
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.Version
import java.time.ZonedDateTime

@Entity
@Table(name = "rental")
class Rental(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rental_id")
    val rentalId: Long = 0L,

    @Column(name = "renter_id", nullable = false)
    val renterId: Long,

    @Column(name = "lender_id", nullable = false)
    val lenderId: Long,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: RentalStatus = RentalStatus.REQUESTED,

    @Column(name = "start_date", nullable = false)
    val startDate: ZonedDateTime,

    @Column(name = "end_date", nullable = false)
    val endDate: ZonedDateTime,

    @Column(name = "total_amount", nullable = false)
    val totalAmount: Long,

    @Column(name = "deposit_amount", nullable = false)
    val depositAmount: Long = 0L,

    @Column(name = "order_id", length = 100, nullable = false)
    var orderId: String = "",

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    var cancelReason: String? = null,

    @Column(name = "requested_at", nullable = false)
    var requestedAt: ZonedDateTime = ZonedDateTime.now(),

    @Column(name = "approved_at")
    var approvedAt: ZonedDateTime? = null,

    @Column(name = "paid_at")
    var paidAt: ZonedDateTime? = null,

    @Column(name = "started_at")
    var startedAt: ZonedDateTime? = null,

    @Column(name = "returned_at")
    var returnedAt: ZonedDateTime? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: ZonedDateTime? = null,

    @Embedded
    var deliveryInfo: DeliveryInfo,

    @Version
    @Column(name = "version", nullable = false)
    val version: Long = 0L,

) : BaseEntity() {

    @Transient
    // JPA/Hibernate는 리플렉션으로 엔티티를 로드할 때 @Transient 필드의
    // initializer를 실행하지 않아 null이 될 수 있음 (BLK-001).
    // private var + lazy getter 패턴으로 null-safe 보장.
    private var _domainEvents: MutableList<DomainEvent>? = null

    private val domainEvents: MutableList<DomainEvent>
        get() = _domainEvents ?: mutableListOf<DomainEvent>().also { _domainEvents = it }

    fun pullEvents(): List<DomainEvent> {
        val events = domainEvents.toList()
        domainEvents.clear()
        return events
    }

    private fun publishEvent(prevStatus: RentalStatus?, newStatus: RentalStatus) {
        domainEvents.add(
            RentalStatusChangedEvent(
                rentalId = rentalId,
                prevStatus = prevStatus,
                newStatus = newStatus,
                renterId = renterId,
                lenderId = lenderId,
                productId = productId,
            ),
        )
    }

    /**
     * 대여 승인 (REQUESTED → APPROVED)
     * 동일 상태 재호출은 멱등 처리 (UT-R12).
     */
    fun approve() {
        if (status == RentalStatus.APPROVED) return
        check(status == RentalStatus.REQUESTED) {
            throw InvalidStateTransitionException(
                "approve() 호출 불가: 현재 상태=$status (REQUESTED 상태에서만 가능)",
            )
        }
        val prev = status
        status = RentalStatus.APPROVED
        approvedAt = ZonedDateTime.now()
        publishEvent(prev, RentalStatus.APPROVED)
    }

    /**
     * 대여 거절 (REQUESTED → CANCELLED)
     */
    fun reject(reason: String) {
        check(status == RentalStatus.REQUESTED) {
            throw InvalidStateTransitionException(
                "reject() 호출 불가: 현재 상태=$status (REQUESTED 상태에서만 가능)",
            )
        }
        val prev = status
        status = RentalStatus.CANCELLED
        cancelReason = reason
        cancelledAt = ZonedDateTime.now()
        publishEvent(prev, RentalStatus.CANCELLED)
    }

    /**
     * 결제 완료 처리 (APPROVED → PAID)
     */
    fun markPaid() {
        check(status == RentalStatus.APPROVED) {
            throw InvalidStateTransitionException(
                "markPaid() 호출 불가: 현재 상태=$status (APPROVED 상태에서만 가능)",
            )
        }
        val prev = status
        status = RentalStatus.PAID
        paidAt = ZonedDateTime.now()
        publishEvent(prev, RentalStatus.PAID)
    }

    /**
     * 대여 시작 / 배송 시작 (PAID → IN_USE)
     */
    fun startRental() {
        check(status == RentalStatus.PAID) {
            throw InvalidStateTransitionException(
                "startRental() 호출 불가: 현재 상태=$status (PAID 상태에서만 가능)",
            )
        }
        val prev = status
        status = RentalStatus.IN_USE
        startedAt = ZonedDateTime.now()
        publishEvent(prev, RentalStatus.IN_USE)
    }

    /**
     * 반납 처리 (IN_USE → RETURNED)
     */
    fun returnRental() {
        check(status == RentalStatus.IN_USE) {
            throw InvalidStateTransitionException(
                "returnRental() 호출 불가: 현재 상태=$status (IN_USE 상태에서만 가능)",
            )
        }
        val prev = status
        status = RentalStatus.RETURNED
        returnedAt = ZonedDateTime.now()
        publishEvent(prev, RentalStatus.RETURNED)
    }

    /**
     * 취소 (REQUESTED 또는 APPROVED → CANCELLED)
     */
    fun cancel(reason: String) {
        check(status == RentalStatus.REQUESTED || status == RentalStatus.APPROVED) {
            throw InvalidStateTransitionException(
                "cancel() 호출 불가: 현재 상태=$status (REQUESTED 또는 APPROVED 상태에서만 가능)",
            )
        }
        val prev = status
        status = RentalStatus.CANCELLED
        cancelReason = reason
        cancelledAt = ZonedDateTime.now()
        publishEvent(prev, RentalStatus.CANCELLED)
    }

    companion object {
        fun request(
            renterId: Long,
            lenderId: Long,
            productId: Long,
            startDate: ZonedDateTime,
            endDate: ZonedDateTime,
            totalAmount: Long,
            depositAmount: Long,
            deliveryInfo: DeliveryInfo,
        ): Rental {
            val rental = Rental(
                renterId = renterId,
                lenderId = lenderId,
                productId = productId,
                startDate = startDate,
                endDate = endDate,
                totalAmount = totalAmount,
                depositAmount = depositAmount,
                deliveryInfo = deliveryInfo,
                requestedAt = ZonedDateTime.now(),
                status = RentalStatus.REQUESTED,
            )
            rental.publishEvent(null, RentalStatus.REQUESTED)
            return rental
        }
    }
}
