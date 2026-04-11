package com.rental.commerce.infrastructure.product

import com.rental.commerce.domain.product.ProductPrice
import org.springframework.data.jpa.repository.JpaRepository

interface ProductPriceJpaRepository : JpaRepository<ProductPrice, Long>
