package com.rental.commerce.infrastructure.product

import com.querydsl.jpa.impl.JPAQueryFactory
import com.rental.commerce.domain.product.ProductImage
import com.rental.commerce.domain.product.ProductImageRepository
import com.rental.commerce.domain.product.QProductImage
import org.springframework.stereotype.Repository

@Repository
class ProductImageRepositoryImpl(
    private val productImageJpaRepository: ProductImageJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : ProductImageRepository {

    private val productImage = QProductImage.productImage

    override fun findByProductId(productId: Long): List<ProductImage> {
        return queryFactory
            .selectFrom(productImage)
            .where(productImage.productId.eq(productId))
            .orderBy(productImage.sortOrder.asc())
            .fetch()
    }

    override fun findByProductIdIn(productIds: List<Long>): List<ProductImage> {
        if (productIds.isEmpty()) return emptyList()
        return queryFactory
            .selectFrom(productImage)
            .where(productImage.productId.`in`(productIds))
            .orderBy(productImage.sortOrder.asc())
            .fetch()
    }

    override fun saveAll(images: List<ProductImage>): List<ProductImage> {
        return productImageJpaRepository.saveAll(images)
    }

    override fun deleteByProductId(productId: Long) {
        queryFactory
            .delete(productImage)
            .where(productImage.productId.eq(productId))
            .execute()
    }

    override fun deleteByProductIdAndObjectKey(productId: Long, objectKey: String) {
        queryFactory
            .delete(productImage)
            .where(
                productImage.productId.eq(productId),
                productImage.objectKey.eq(objectKey),
            )
            .execute()
    }
}
