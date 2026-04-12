package com.rental.commerce.infrastructure.product

import com.querydsl.jpa.impl.JPAQueryFactory
import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.product.ProductSearchCondition
import com.rental.commerce.domain.product.QProduct
import org.springframework.data.domain.Page
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
    private val queryFactory: JPAQueryFactory,
    private val productSearchRepository: ProductSearchRepositoryImpl,
) : ProductRepository {

    private val product = QProduct.product

    override fun save(product: Product): Product {
        return productJpaRepository.save(product)
    }

    override fun findById(productId: Long): Product? {
        return queryFactory
            .selectFrom(product)
            .where(product.productId.eq(productId))
            .fetchOne()
    }

    override fun findByUserId(userId: Long): List<Product> {
        return queryFactory
            .selectFrom(product)
            .where(product.userId.eq(userId))
            .fetch()
    }

    override fun search(condition: ProductSearchCondition): Page<Product> {
        return productSearchRepository.search(condition)
    }
}
