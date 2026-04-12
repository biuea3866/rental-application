package com.rental.commerce.infrastructure.user

import com.rental.commerce.domain.user.RenterProfile
import org.springframework.data.jpa.repository.JpaRepository

interface RenterProfileJpaRepository : JpaRepository<RenterProfile, Long>
