package com.rental.commerce.domain.rental

enum class PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED,
    ;

    fun canTransitTo(target: PaymentStatus): Boolean {
        return allowedTransitions[this]?.contains(target) ?: false
    }

    companion object {
        private val allowedTransitions: Map<PaymentStatus, Set<PaymentStatus>> = mapOf(
            PENDING to setOf(COMPLETED, FAILED),
            COMPLETED to setOf(REFUNDED),
        )
    }
}
