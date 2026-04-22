package com.rental.commerce.application.wishlist

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.wishlist.WishlistDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetMyWishlistUseCase(
    private val wishlistDomainService: WishlistDomainService,
) {
    fun execute(userId: Long, pageQuery: PageQuery): WishlistPageResult =
        WishlistPageResult.from(wishlistDomainService.getMyWishlist(userId, pageQuery))
}
