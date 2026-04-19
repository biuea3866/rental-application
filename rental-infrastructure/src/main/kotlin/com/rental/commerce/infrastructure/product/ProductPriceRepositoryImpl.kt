package com.rental.commerce.infrastructure.product

import com.querydsl.jpa.impl.JPAQueryFactory
import com.rental.commerce.domain.product.ProductPrice
import com.rental.commerce.domain.product.ProductPriceRepository
import com.rental.commerce.domain.product.QProductPrice
import org.springframework.stereotype.Repository

@Repository
class ProductPriceRepositoryImpl(
    private val productPriceJpaRepository: ProductPriceJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : ProductPriceRepository {

    private val productPrice = QProductPrice.productPrice

    override fun findByProductId(productId: Long): List<ProductPrice> {
        return queryFactory
            .selectFrom(productPrice)
            .where(productPrice.productId.eq(productId))
            .fetch()
    }

    override fun saveAll(prices: List<ProductPrice>): List<ProductPrice> {
        return productPriceJpaRepository.saveAll(prices)
    }

    override fun deleteByProductId(productId: Long) {
        queryFactory
            .delete(productPrice)
            .where(productPrice.productId.eq(productId))
            .execute()
    }
}
