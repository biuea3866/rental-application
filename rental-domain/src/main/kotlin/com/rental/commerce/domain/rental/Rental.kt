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
class Rental private constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L,

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
    val depositAmount: Long,

    @Embedded
    val deliveryInfo: DeliveryInfo,

    @Column(name = "cancel_reason", length = 500)
    var cancelReason: String? = null,

    @Column(name = "requested_at", nullable = false)
    val requestedAt: ZonedDateTime = ZonedDateTime.now(),

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

    @Version
    @Column(name = "version", nullable = false)
    val version: Int = 0,

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

    fun isOwnedByLender(userId: Long): Boolean = lenderId == userId

    fun isRequestedByRenter(userId: Long): Boolean = renterId == userId

    fun isParticipant(userId: Long): Boolean = renterId == userId || lenderId == userId

    fun approve() {
        validateTransition(RentalStatus.APPROVED)
        val previousStatus = status
        this.status = RentalStatus.APPROVED
        this.approvedAt = ZonedDateTime.now()
        publishStatusChangedEvent(from = previousStatus, to = RentalStatus.APPROVED)
    }

    fun reject(reason: String) {
        if (status != RentalStatus.REQUESTED) {
            throw InvalidStateTransitionException(
                "${status.name}에서 CANCELLED(으)로 거절 전이는 REQUESTED 상태에서만 가능합니다"
            )
        }
        val previousStatus = status
        this.status = RentalStatus.CANCELLED
        this.cancelReason = reason
        this.cancelledAt = ZonedDateTime.now()
        publishStatusChangedEvent(from = previousStatus, to = RentalStatus.CANCELLED)
    }

    fun markPaid() {
        validateTransition(RentalStatus.PAID)
        this.status = RentalStatus.PAID
        this.paidAt = ZonedDateTime.now()
    }

    fun startRental() {
        validateTransition(RentalStatus.IN_USE)
        val previousStatus = status
        this.status = RentalStatus.IN_USE
        this.startedAt = ZonedDateTime.now()
        publishStatusChangedEvent(from = previousStatus, to = RentalStatus.IN_USE)
    }

    fun returnRental() {
        validateTransition(RentalStatus.RETURNED)
        val previousStatus = status
        this.status = RentalStatus.RETURNED
        this.returnedAt = ZonedDateTime.now()
        publishStatusChangedEvent(from = previousStatus, to = RentalStatus.RETURNED)
    }

    fun cancel(reason: String) {
        validateTransition(RentalStatus.CANCELLED)
        val previousStatus = status
        this.status = RentalStatus.CANCELLED
        this.cancelReason = reason
        this.cancelledAt = ZonedDateTime.now()
        publishStatusChangedEvent(from = previousStatus, to = RentalStatus.CANCELLED)
    }

    private fun validateTransition(target: RentalStatus) {
        if (!status.canTransitTo(target)) {
            throw InvalidStateTransitionException(
                "${status.name}에서 ${target.name}(으)로 전이할 수 없습니다"
            )
        }
    }

    private fun publishStatusChangedEvent(from: RentalStatus, to: RentalStatus) {
        domainEvents.add(
            RentalStatusChangedEvent(
                rentalId = id,
                renterId = renterId,
                lenderId = lenderId,
                fromStatus = from,
                toStatus = to,
            )
        )
    }

    companion object {
        fun create(
            renterId: Long,
            lenderId: Long,
            productId: Long,
            startDate: ZonedDateTime,
            endDate: ZonedDateTime,
            totalAmount: Long,
            depositAmount: Long,
            deliveryInfo: DeliveryInfo,
        ): Rental {
            return Rental(
                renterId = renterId,
                lenderId = lenderId,
                productId = productId,
                startDate = startDate,
                endDate = endDate,
                totalAmount = totalAmount,
                depositAmount = depositAmount,
                deliveryInfo = deliveryInfo,
                status = RentalStatus.REQUESTED,
                requestedAt = ZonedDateTime.now(),
            )
        }
    }
}
