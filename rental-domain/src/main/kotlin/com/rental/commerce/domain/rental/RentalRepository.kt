package com.rental.commerce.domain.rental

import java.time.ZonedDateTime

interface RentalRepository {
    fun save(rental: Rental): Rental
    fun findById(rentalId: Long): Rental?
    fun existsOverlappingRental(
        productId: Long,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime,
        excludeRentalId: Long? = null,
    ): Boolean
    fun findAllByRenterIdOrLenderId(renterId: Long, lenderId: Long): List<Rental>
}
