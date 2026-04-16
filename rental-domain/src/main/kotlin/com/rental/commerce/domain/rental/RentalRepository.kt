package com.rental.commerce.domain.rental

import java.time.ZonedDateTime

interface RentalRepository {

    fun save(rental: Rental): Rental

    fun findById(rentalId: Long): Rental?

    /**
     * 해당 상품에 대해 주어진 기간과 겹치는 활성 대여(CANCELLED 제외)가 존재하는지 확인.
     * SQL 조건: start_date < endDate AND end_date > startDate AND status != 'CANCELLED'
     */
    fun existsOverlappingRental(
        productId: Long,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime,
    ): Boolean

    fun findAllByRenterIdOrLenderId(
        userId: Long,
        pageable: org.springframework.data.domain.Pageable,
    ): org.springframework.data.domain.Page<Rental>
}
