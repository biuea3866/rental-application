package com.rental.commerce.application.wishlist

import com.rental.commerce.domain.wishlist.WishlistDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RemoveWishlistItemUseCase(
    private val wishlistDomainService: WishlistDomainService,
) {
    fun execute(userId: Long, productId: Long) {
        wishlistDomainService.remove(userId, productId)
    }
}
