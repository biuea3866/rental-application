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

/**
 * ProductSearchRepositoryImpl (BE-410, ADR-010).
 *
 * Sprint 4 확장:
 *  - categoryCodes 다중 필터 (기존 categoryCode 는 fallback)
 *  - regionCode 필터 (product.region_code)
 *  - RATING_AVG / RENTAL_COUNT 정렬 (비정규화 컬럼, BE-411 리스너가 갱신)
 *  - 가격 필터: base_price_amount 를 1차로 쓰고, product_price join 은 rentalUnit 지정 시만.
 */
@Component
class ProductSearchRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) {

    private val product = QProduct.product
    private val productPrice = QProductPrice.productPrice

    fun search(condition: ProductSearchCondition): Page<Product> {
        val pageable = PageRequest.of(condition.page, condition.size)
        val needsPriceJoin = condition.rentalUnit != null
        val whereClause = buildSearchCondition(condition, needsPriceJoin)

        val totalCount = queryFactory
            .select(product.countDistinct())
            .from(product)
            .apply { applyPriceJoinIfNeeded(this, needsPriceJoin) }
            .where(whereClause)
            .fetchOne() ?: 0L

        val content = queryFactory
            .selectFrom(product)
            .distinct()
            .apply { applyPriceJoinIfNeeded(this, needsPriceJoin) }
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
    ) {
        if (needsPriceJoin) {
            query.leftJoin(productPrice).on(productPrice.productId.eq(product.productId))
        }
    }

    private fun buildSearchCondition(
        condition: ProductSearchCondition,
        usesPriceJoin: Boolean,
    ): BooleanBuilder {
        val builder = BooleanBuilder()

        condition.keyword?.takeIf { it.isNotBlank() }?.let { keyword ->
            builder.and(
                product.name.containsIgnoreCase(keyword)
                    .or(product.description.containsIgnoreCase(keyword))
            )
        }

        // categoryCodes 가 있으면 우선 사용 (다중 필터).
        when {
            !condition.categoryCodes.isNullOrEmpty() ->
                builder.and(product.categoryCode.`in`(condition.categoryCodes))
            !condition.categoryCode.isNullOrBlank() ->
                builder.and(product.categoryCode.eq(condition.categoryCode))
        }

        condition.status?.let { status ->
            builder.and(product.status.eq(status))
        }

        condition.regionCode?.takeIf { it.isNotBlank() }?.let { region ->
            builder.and(product.regionCode.eq(region))
        }

        // 가격 필터
        if (usesPriceJoin) {
            condition.rentalUnit?.let { unit ->
                builder.and(productPrice.rentalUnit.eq(unit))
            }
            condition.minPrice?.let { builder.and(productPrice.priceAmount.goe(it)) }
            condition.maxPrice?.let { builder.and(productPrice.priceAmount.loe(it)) }
        } else {
            // 기본가 (base_price_amount) 기준 필터 — 커버링 인덱스 활용 (ADR-010)
            condition.minPrice?.let { builder.and(product.basePriceAmount.goe(it)) }
            condition.maxPrice?.let { builder.and(product.basePriceAmount.loe(it)) }
        }

        return builder
    }

    private fun resolveOrderSpecifier(sortBy: ProductSortBy, sortDirection: SortDirection): OrderSpecifier<*> {
        val isAsc = sortDirection == SortDirection.ASC
        return when (sortBy) {
            ProductSortBy.NAME -> if (isAsc) product.name.asc() else product.name.desc()
            ProductSortBy.DEPOSIT_AMOUNT -> if (isAsc) product.depositAmount.asc() else product.depositAmount.desc()
            ProductSortBy.CREATED_AT -> if (isAsc) product.createdAt.asc() else product.createdAt.desc()
            ProductSortBy.RATING_AVG -> if (isAsc) product.ratingAvg.asc() else product.ratingAvg.desc()
            ProductSortBy.RENTAL_COUNT -> if (isAsc) product.rentalCount.asc() else product.rentalCount.desc()
        }
    }
}
