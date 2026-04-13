package com.rental.commerce.domain.product

enum class ProductStatus {
    DRAFT,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    AVAILABLE,
    RENTED,
    DELETED,
    ;

    fun canTransitTo(target: ProductStatus): Boolean {
        return allowedTransitions[this]?.contains(target) ?: false
    }

    companion object {
        private val allowedTransitions: Map<ProductStatus, Set<ProductStatus>> = mapOf(
            DRAFT to setOf(UNDER_REVIEW, DELETED),
            UNDER_REVIEW to setOf(APPROVED, REJECTED),
            APPROVED to setOf(AVAILABLE),
            REJECTED to setOf(DRAFT, DELETED),
            AVAILABLE to setOf(RENTED),
            RENTED to setOf(AVAILABLE),
        )
    }
}
