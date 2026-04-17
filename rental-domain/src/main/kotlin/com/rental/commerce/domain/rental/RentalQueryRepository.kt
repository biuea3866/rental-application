package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.PageResult

/**
 * RentalQueryRepository — 복잡한 조회 전용 Port.
 *
 * - 내 대여 목록: renterId 또는 lenderId 기준, 상태 필터, 페이지네이션
 * - 대여 상세: rentalId 기준 Rental + RentalPayment JOIN
 *
 * 구현체는 rental-infrastructure에서 QueryDSL JPAQueryFactory로 제공한다.
 */
interface RentalQueryRepository {

    /**
     * 내 대여 목록 조회 — renterId 또는 lenderId 기준, 상태 필터, 페이지네이션.
     */
    fun findMyRentals(condition: RentalQueryCondition): PageResult<Rental>

    /**
     * 대여 상세 조회 — rentalId 기준 Rental + RentalPayment JOIN.
     * 대여가 없으면 null 반환.
     */
    fun findRentalWithPayment(rentalId: Long): RentalWithPayment?
}

/**
 * Rental + RentalPayment 통합 조회 결과.
 * payment는 아직 결제가 없는 경우 null.
 */
data class RentalWithPayment(
    val rental: Rental,
    val payment: RentalPayment?,
)
