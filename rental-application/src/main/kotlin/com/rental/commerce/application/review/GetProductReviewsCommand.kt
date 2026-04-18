package com.rental.commerce.application.review

import com.rental.commerce.domain.common.PageQuery

data class GetProductReviewsCommand(
    val productId: Long,
    val pageQuery: PageQuery,
)
