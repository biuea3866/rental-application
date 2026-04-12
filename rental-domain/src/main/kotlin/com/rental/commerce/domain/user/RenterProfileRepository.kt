package com.rental.commerce.domain.user

interface RenterProfileRepository {

    fun save(profile: RenterProfile): RenterProfile

    fun findByUserId(userId: Long): RenterProfile?

    fun existsByUserId(userId: Long): Boolean
}
