package com.rental.commerce.domain.rental

interface RentalPaymentRepository {
    fun save(rentalPayment: RentalPayment): RentalPayment
    fun findByRentalId(rentalId: Long): RentalPayment?
    fun existsByExternalPaymentId(externalPaymentId: String): Boolean
}
