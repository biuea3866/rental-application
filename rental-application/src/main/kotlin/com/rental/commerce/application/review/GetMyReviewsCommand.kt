package com.rental.commerce.application.review

import com.rental.commerce.domain.common.PageQuery

data class GetMyReviewsCommand(
    val renterId: Long,
    val pageQuery: PageQuery,
)
