package com.rental.commerce.infrastructure.product

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductSearchCondition
import com.rental.commerce.domain.product.ProductSortBy
import com.rental.commerce.domain.product.QProduct
import com.rental.commerce.domain.product.QProductPrice
import com.rental.commerce.domain.product.SortDirection
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class ProductSearchRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) {

    private val product = QProduct.product
    private val productPrice = QProductPrice.productPrice

    fun search(condition: ProductSearchCondition): Page<Product> {
        val pageable = PageRequest.of(condition.page, condition.size)
        val whereClause = buildSearchCondition(condition)
        val needsPriceJoin = condition.minPrice != null || condition.maxPrice != null

        val totalCount = queryFactory
            .select(product.countDistinct())
            .from(product)
            .apply { applyPriceJoinIfNeeded(this, needsPriceJoin, condition) }
            .where(whereClause)
            .fetchOne() ?: 0L

        val content = queryFactory
            .selectFrom(product)
            .distinct()
            .apply { applyPriceJoinIfNeeded(this, needsPriceJoin, condition) }
            .where(whereClause)
            .orderBy(resolveOrderSpecifier(condition.sortBy, condition.sortDirection))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        return PageImpl(content, pageable, totalCount)
    }

    private fun <T> applyPriceJoinIfNeeded(
        query: JPAQuery<T>,
        needsPriceJoin: Boolean,
        condition: ProductSearchCondition,
    ) {
        if (needsPriceJoin) {
            query.leftJoin(productPrice).on(productPrice.productId.eq(product.productId))
            condition.rentalUnit?.let { unit ->
                query.where(productPrice.rentalUnit.eq(unit))
            }
        }
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

    private fun resolveOrderSpecifier(sortBy: ProductSortBy, sortDirection: SortDirection): OrderSpecifier<*> {
        val isAsc = sortDirection == SortDirection.ASC

        return when (sortBy) {
            ProductSortBy.NAME -> if (isAsc) product.name.asc() else product.name.desc()
            ProductSortBy.DEPOSIT_AMOUNT -> if (isAsc) product.depositAmount.asc() else product.depositAmount.desc()
            ProductSortBy.CREATED_AT -> if (isAsc) product.createdAt.asc() else product.createdAt.desc()
        }
    }
}
