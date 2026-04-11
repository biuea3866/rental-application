package com.rental.commerce.domain.priceguide

import com.rental.commerce.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "category_price_guide")
class CategoryPriceGuide(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_price_guide_id")
    val categoryPriceGuideId: Long = 0L,

    @Column(name = "category_code", nullable = false, length = 50)
    val categoryCode: String,

    @Column(name = "rental_unit", nullable = false, length = 20)
    val rentalUnit: String,

    @Column(name = "min_price", nullable = false)
    val minPrice: Long,

    @Column(name = "max_price", nullable = false)
    val maxPrice: Long,

) : BaseEntity()
