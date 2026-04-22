package com.rental.commerce.infrastructure.wishlist.mysql

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.wishlist.Wishlist
import com.rental.commerce.domain.wishlist.WishlistRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import kotlin.math.ceil

@Repository
class WishlistRepositoryImpl(
    private val wishlistJpaRepository: WishlistJpaRepository,
) : WishlistRepository {

    override fun save(wishlist: Wishlist): Wishlist = wishlistJpaRepository.save(wishlist)

    override fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean =
        wishlistJpaRepository.existsByUserIdAndProductId(userId, productId)

    override fun findByUserIdAndProductId(userId: Long, productId: Long): Wishlist? =
        wishlistJpaRepository.findByUserIdAndProductId(userId, productId)

    override fun deleteByUserIdAndProductId(userId: Long, productId: Long): Long =
        wishlistJpaRepository.deleteByUserIdAndProductId(userId, productId)

    override fun findByUserId(userId: Long, pageQuery: PageQuery): PageResult<Wishlist> {
        val page = wishlistJpaRepository.findAllByUserIdOrderByCreatedAtDesc(
            userId,
            PageRequest.of(pageQuery.page, pageQuery.size),
        )
        return PageResult(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = if (page.totalElements == 0L) 0
            else ceil(page.totalElements.toDouble() / pageQuery.size).toInt(),
        )
    }

    override fun findUserIdsByProductId(productId: Long): List<Long> =
        wishlistJpaRepository.findAllByProductId(productId).map { it.userId }
}
