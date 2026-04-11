package com.rental.commerce.infrastructure.product

import com.rental.commerce.domain.product.ProductImage
import org.springframework.data.jpa.repository.JpaRepository

interface ProductImageJpaRepository : JpaRepository<ProductImage, Long>
