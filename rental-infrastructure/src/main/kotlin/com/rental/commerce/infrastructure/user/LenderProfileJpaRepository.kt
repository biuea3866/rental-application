package com.rental.commerce.infrastructure.user

import com.rental.commerce.domain.user.LenderProfile
import org.springframework.data.jpa.repository.JpaRepository

interface LenderProfileJpaRepository : JpaRepository<LenderProfile, Long>
