package com.rental.commerce.domain.rental

interface RentalPaymentRepository {
    fun save(payment: RentalPayment): RentalPayment
    fun findByRentalId(rentalId: Long): RentalPayment?
}
