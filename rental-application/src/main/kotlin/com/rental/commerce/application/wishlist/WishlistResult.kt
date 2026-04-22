package com.rental.commerce.application.wishlist

import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.wishlist.Wishlist
import java.time.ZonedDateTime

data class WishlistResult(
    val id: Long,
    val userId: Long,
    val productId: Long,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(wishlist: Wishlist) = WishlistResult(
            id = wishlist.id,
            userId = wishlist.userId,
            productId = wishlist.productId,
            createdAt = wishlist.createdAt,
        )
    }
}

data class WishlistPageResult(
    val items: List<WishlistResult>,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun from(page: PageResult<Wishlist>) = WishlistPageResult(
            items = page.content.map { WishlistResult.from(it) },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
