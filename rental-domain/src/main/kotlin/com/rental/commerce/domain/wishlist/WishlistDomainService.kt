package com.rental.commerce.domain.wishlist

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import org.springframework.stereotype.Service

/**
 * WishlistDomainService (BE-420, PRD-004 §4.4).
 *
 * 중복 추가 방지 (애플리케이션 + DB UNIQUE 이중 방어).
 * 사용자 본인만 조작 가능 — 권한 체크는 UseCase/Controller 경계에서 수행.
 */
@Service
class WishlistDomainService(
    private val wishlistRepository: WishlistRepository,
) {
    fun add(userId: Long, productId: Long): Wishlist {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw WishlistAlreadyExistsException()
        }
        return wishlistRepository.save(Wishlist.create(userId, productId))
    }

    fun remove(userId: Long, productId: Long) {
        val removed = wishlistRepository.deleteByUserIdAndProductId(userId, productId)
        if (removed == 0L) throw WishlistNotFoundException()
    }

    fun getMyWishlist(userId: Long, pageQuery: PageQuery): PageResult<Wishlist> =
        wishlistRepository.findByUserId(userId, pageQuery)

    fun findUserIdsWithProduct(productId: Long): List<Long> =
        wishlistRepository.findUserIdsByProductId(productId)
}
