package com.rental.commerce.domain.common

enum class RentalStatus {
    REQUESTED,
    APPROVED,
    PAID,
    IN_USE,
    RETURNED,
    CANCELLED,
    ;

    fun canTransitTo(target: RentalStatus): Boolean {
        return allowedTransitions[this]?.contains(target) ?: false
    }

    companion object {
        private val allowedTransitions: Map<RentalStatus, Set<RentalStatus>> = mapOf(
            REQUESTED to setOf(APPROVED, CANCELLED),
            APPROVED to setOf(PAID, CANCELLED),
            PAID to setOf(IN_USE, CANCELLED),
            IN_USE to setOf(RETURNED),
        )
    }
}
