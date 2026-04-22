package com.rental.commerce.domain.wishlist

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult

interface WishlistRepository {

    fun save(wishlist: Wishlist): Wishlist

    fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean

    fun findByUserIdAndProductId(userId: Long, productId: Long): Wishlist?

    fun deleteByUserIdAndProductId(userId: Long, productId: Long): Long

    fun findByUserId(userId: Long, pageQuery: PageQuery): PageResult<Wishlist>

    fun findUserIdsByProductId(productId: Long): List<Long>
}
