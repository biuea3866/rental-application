package com.rental.commerce.infrastructure.product

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.jpa.impl.JPAQueryFactory
import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.product.ProductSearchCondition
import com.rental.commerce.domain.product.QProduct
import com.rental.commerce.domain.product.QProductPrice
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : ProductRepository {

    private val product = QProduct.product
    private val productPrice = QProductPrice.productPrice

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
        val pageable = PageRequest.of(condition.page, condition.size)
        val whereClause = buildSearchCondition(condition)
        val needsPriceJoin = condition.minPrice != null || condition.maxPrice != null

        val query = queryFactory
            .selectFrom(product)
            .distinct()

        if (needsPriceJoin) {
            query.leftJoin(productPrice).on(productPrice.productId.eq(product.productId))
            condition.rentalUnit?.let { unit ->
                query.where(productPrice.rentalUnit.eq(unit))
            }
        }

        query.where(whereClause)

        val totalCount = query.fetch().size.toLong()

        val results = queryFactory
            .selectFrom(product)
            .distinct()

        if (needsPriceJoin) {
            results.leftJoin(productPrice).on(productPrice.productId.eq(product.productId))
            condition.rentalUnit?.let { unit ->
                results.where(productPrice.rentalUnit.eq(unit))
            }
        }

        val content = results
            .where(whereClause)
            .orderBy(resolveOrderSpecifier(condition.sortBy, condition.sortDirection))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        return PageImpl(content, pageable, totalCount)
    }

    private fun buildSearchCondition(condition: ProductSearchCondition): BooleanBuilder {
        val builder = BooleanBuilder()

        condition.keyword?.takeIf { it.isNotBlank() }?.let { keyword ->
            builder.and(
                product.name.containsIgnoreCase(keyword)
                    .or(product.description.containsIgnoreCase(keyword))
            )
        }

        condition.categoryCode?.takeIf { it.isNotBlank() }?.let { code ->
            builder.and(product.categoryCode.eq(code))
        }

        condition.status?.let { status ->
            builder.and(product.status.eq(status))
        }

        condition.minPrice?.let { min ->
            builder.and(productPrice.priceAmount.goe(min))
        }

        condition.maxPrice?.let { max ->
            builder.and(productPrice.priceAmount.loe(max))
        }

        return builder
    }

    private fun resolveOrderSpecifier(sortBy: String, sortDirection: String): OrderSpecifier<*> {
        val isAsc = sortDirection.equals("ASC", ignoreCase = true)

        return when (sortBy) {
            "name" -> if (isAsc) product.name.asc() else product.name.desc()
            "depositAmount" -> if (isAsc) product.depositAmount.asc() else product.depositAmount.desc()
            else -> if (isAsc) product.createdAt.asc() else product.createdAt.desc()
        }
    }
}
