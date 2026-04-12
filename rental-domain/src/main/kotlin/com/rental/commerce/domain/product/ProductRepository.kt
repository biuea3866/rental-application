package com.rental.commerce.domain.product

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductRepository {

    fun save(product: Product): Product

    fun findById(productId: Long): Product?

    fun findByUserId(userId: Long): List<Product>

    fun findByUserIdAndStatusNot(
        userId: Long,
        excludeStatus: ProductStatus,
        pageable: Pageable,
    ): Page<Product>

    fun search(condition: ProductSearchCondition): Page<Product>
}
