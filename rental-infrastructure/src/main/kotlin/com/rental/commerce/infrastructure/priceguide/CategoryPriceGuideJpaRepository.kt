package com.rental.commerce.infrastructure.priceguide

import com.rental.commerce.domain.priceguide.CategoryPriceGuide
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryPriceGuideJpaRepository : JpaRepository<CategoryPriceGuide, Long>
