package com.rental.commerce.domain.user

enum class UserRole {
    LENDER,
    RENTER,
    BOTH;

    fun hasLenderRole(): Boolean = this == LENDER || this == BOTH

    fun hasRenterRole(): Boolean = this == RENTER || this == BOTH
}
