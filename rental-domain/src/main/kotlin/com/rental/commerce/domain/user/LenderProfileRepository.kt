package com.rental.commerce.domain.user

interface LenderProfileRepository {

    fun save(profile: LenderProfile): LenderProfile

    fun findByUserId(userId: Long): LenderProfile?

    fun existsByUserId(userId: Long): Boolean
}
