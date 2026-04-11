package com.rental.commerce.infrastructure.product

import com.rental.commerce.domain.product.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductJpaRepository : JpaRepository<Product, Long>
