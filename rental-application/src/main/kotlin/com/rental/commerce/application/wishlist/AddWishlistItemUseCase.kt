package com.rental.commerce.application.wishlist

import com.rental.commerce.domain.wishlist.WishlistDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AddWishlistItemUseCase(
    private val wishlistDomainService: WishlistDomainService,
) {
    fun execute(userId: Long, productId: Long): WishlistResult =
        WishlistResult.from(wishlistDomainService.add(userId, productId))
}
