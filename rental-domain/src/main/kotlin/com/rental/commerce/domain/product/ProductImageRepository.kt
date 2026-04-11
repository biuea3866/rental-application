package com.rental.commerce.domain.product

interface ProductImageRepository {

    fun findByProductId(productId: Long): List<ProductImage>

    fun saveAll(images: List<ProductImage>): List<ProductImage>

    fun deleteByProductId(productId: Long)

    fun deleteByProductIdAndObjectKey(productId: Long, objectKey: String)
}
