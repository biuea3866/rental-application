package com.rental.commerce.infrastructure.priceguide

import com.rental.commerce.domain.priceguide.CategoryPriceGuide
import com.rental.commerce.domain.priceguide.CategoryPriceGuideRepository
import com.rental.commerce.domain.priceguide.QCategoryPriceGuide
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class CategoryPriceGuideRepositoryImpl(
    private val jpaQueryFactory: JPAQueryFactory,
) : CategoryPriceGuideRepository {

    private val categoryPriceGuide = QCategoryPriceGuide.categoryPriceGuide

    override fun findByCategoryCode(categoryCode: String): List<CategoryPriceGuide> {
        return jpaQueryFactory
            .selectFrom(categoryPriceGuide)
            .where(categoryPriceGuide.categoryCode.eq(categoryCode))
            .fetch()
    }
}
