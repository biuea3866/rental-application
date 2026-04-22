package com.rental.commerce.domain.product

/**
 * 상품 검색 정렬 축 (ADR-010).
 *
 * RATING_AVG / RENTAL_COUNT 는 Sprint 4 비정규화 컬럼 기반 정렬(BE-410).
 */
enum class ProductSortBy {
    CREATED_AT,
    NAME,
    DEPOSIT_AMOUNT,
    RATING_AVG,
    RENTAL_COUNT,
}

enum class SortDirection {
    ASC,
    DESC,
}

/**
 * 상품 검색 조건 VO.
 *
 * Sprint 4 추가 필드 (BE-410):
 *  - regionCode : 지역 필터 (product.region_code)
 *  - categoryCodes : 다중 카테고리 필터 (OR)
 *
 * 기존 categoryCode(단일)는 레거시 호환용 — categoryCodes 가 우선.
 */
data class ProductSearchCondition(
    val keyword: String? = null,
    val categoryCode: String? = null,
    val categoryCodes: List<String>? = null,
    val status: ProductStatus? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val rentalUnit: RentalUnit? = null,
    val regionCode: String? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: ProductSortBy = ProductSortBy.CREATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC,
) {
    init {
        require(page >= 0) { "page 는 0 이상이어야 합니다" }
        require(size in 1..100) { "size 는 1~100 사이여야 합니다" }
        if (minPrice != null && maxPrice != null) {
            require(minPrice <= maxPrice) { "minPrice 는 maxPrice 이하여야 합니다" }
        }
    }
}
