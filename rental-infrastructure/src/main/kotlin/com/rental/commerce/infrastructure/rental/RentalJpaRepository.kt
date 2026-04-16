package com.rental.commerce.infrastructure.rental

import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalStatus
import java.time.ZonedDateTime
import org.springframework.data.jpa.repository.JpaRepository

interface RentalJpaRepository : JpaRepository<Rental, Long> {

    fun findAllByRenterIdOrLenderId(renterId: Long, lenderId: Long): List<Rental>

    fun existsByProductIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        productId: Long,
        statuses: Collection<RentalStatus>,
        endDate: ZonedDateTime,
        startDate: ZonedDateTime,
    ): Boolean

    fun existsByProductIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
        productId: Long,
        statuses: Collection<RentalStatus>,
        endDate: ZonedDateTime,
        startDate: ZonedDateTime,
        excludeId: Long,
    ): Boolean
}
