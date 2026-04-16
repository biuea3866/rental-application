package com.rental.commerce.domain.rental

interface RentalRepository {
    fun save(rental: Rental): Rental
    fun findById(rentalId: Long): Rental?
    fun findByProductIdAndStatusIn(productId: Long, statuses: List<RentalStatus>): List<Rental>
    fun findByRenterIdOrderByRequestedAtDesc(renterId: Long): List<Rental>
    fun findByLenderIdOrderByRequestedAtDesc(lenderId: Long): List<Rental>
}
