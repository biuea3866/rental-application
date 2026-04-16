package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.InvalidStateTransitionException
import java.time.ZonedDateTime

/**
 * Rental Aggregate Root.
 * JPA 어노테이션은 rental-wt-201(RC-201) 브랜치에서 추가됩니다.
 * 이 파일은 UseCase 컴파일을 위한 도메인 모델 스텁입니다.
 */
class Rental(
    val rentalId: Long? = null,
    val renterId: Long,
    val lenderId: Long,
    val productId: Long,
    var status: RentalStatus = RentalStatus.REQUESTED,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val totalAmount: Long,
    val depositAmount: Long = 0L,
    val orderId: String,
    val deliveryInfo: DeliveryInfo,
    var cancelReason: String? = null,
    val requestedAt: ZonedDateTime = ZonedDateTime.now(),
    var approvedAt: ZonedDateTime? = null,
    var paidAt: ZonedDateTime? = null,
    var startedAt: ZonedDateTime? = null,
    var returnedAt: ZonedDateTime? = null,
    var cancelledAt: ZonedDateTime? = null,
    val version: Int = 0,
) {

    fun approve() {
        check(status == RentalStatus.REQUESTED) {
            throw InvalidStateTransitionException("REQUESTED 상태에서만 승인할 수 있습니다 (현재: $status)")
        }
        status = RentalStatus.APPROVED
        approvedAt = ZonedDateTime.now()
    }

    fun reject(reason: String) {
        check(status == RentalStatus.REQUESTED) {
            throw InvalidStateTransitionException("REQUESTED 상태에서만 거절할 수 있습니다 (현재: $status)")
        }
        status = RentalStatus.CANCELLED
        cancelReason = reason
        cancelledAt = ZonedDateTime.now()
    }

    fun markPaid() {
        check(status == RentalStatus.APPROVED) {
            throw InvalidStateTransitionException("APPROVED 상태에서만 결제 완료로 전이할 수 있습니다 (현재: $status)")
        }
        status = RentalStatus.PAID
        paidAt = ZonedDateTime.now()
    }

    fun start() {
        check(status == RentalStatus.PAID) {
            throw InvalidStateTransitionException("PAID 상태에서만 대여 시작할 수 있습니다 (현재: $status)")
        }
        status = RentalStatus.IN_USE
        startedAt = ZonedDateTime.now()
    }

    fun returnRental() {
        check(status == RentalStatus.IN_USE) {
            throw InvalidStateTransitionException("IN_USE 상태에서만 반납할 수 있습니다 (현재: $status)")
        }
        status = RentalStatus.RETURNED
        returnedAt = ZonedDateTime.now()
    }

    fun cancel(reason: String) {
        check(status == RentalStatus.REQUESTED || status == RentalStatus.APPROVED || status == RentalStatus.PAID) {
            throw InvalidStateTransitionException("REQUESTED/APPROVED/PAID 상태에서만 취소할 수 있습니다 (현재: $status)")
        }
        status = RentalStatus.CANCELLED
        cancelReason = reason
        cancelledAt = ZonedDateTime.now()
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
            orderId: String,
            deliveryInfo: DeliveryInfo,
        ): Rental {
            return Rental(
                renterId = renterId,
                lenderId = lenderId,
                productId = productId,
                status = RentalStatus.REQUESTED,
                startDate = startDate,
                endDate = endDate,
                totalAmount = totalAmount,
                depositAmount = depositAmount,
                orderId = orderId,
                deliveryInfo = deliveryInfo,
                requestedAt = ZonedDateTime.now(),
            )
        }
    }
}
