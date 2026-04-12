package com.rental.commerce.domain.common

data class PageQuery(
    val page: Int,
    val size: Int,
)

data class PageResult<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
)
