package com.rental.commerce.presentation.api.wishlist

import com.rental.commerce.application.wishlist.AddWishlistItemUseCase
import com.rental.commerce.application.wishlist.GetMyWishlistUseCase
import com.rental.commerce.application.wishlist.RemoveWishlistItemUseCase
import com.rental.commerce.application.wishlist.WishlistPageResult
import com.rental.commerce.application.wishlist.WishlistResult
import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.presentation.api.common.AuthenticatedMember
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * WishlistApiController (BE-420, PRD-004 §4.4).
 *
 * 사용자 ID 는 @AuthenticatedMember 에서만 주입 — body/쿼리 스푸핑 차단.
 */
@RestController
@RequestMapping("/api/v1/wishlist")
class WishlistApiController(
    private val addWishlistItemUseCase: AddWishlistItemUseCase,
    private val removeWishlistItemUseCase: RemoveWishlistItemUseCase,
    private val getMyWishlistUseCase: GetMyWishlistUseCase,
) {

    @PostMapping("/{productId}")
    fun add(
        @PathVariable productId: Long,
        @AuthenticatedMember userId: Long,
    ): ResponseEntity<WishlistResult> {
        val result = addWishlistItemUseCase.execute(userId, productId)
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @DeleteMapping("/{productId}")
    fun remove(
        @PathVariable productId: Long,
        @AuthenticatedMember userId: Long,
    ): ResponseEntity<Void> {
        removeWishlistItemUseCase.execute(userId, productId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    fun getMyWishlist(
        @AuthenticatedMember userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<WishlistPageResult> {
        val result = getMyWishlistUseCase.execute(userId, PageQuery(page = page, size = size))
        return ResponseEntity.ok(result)
    }
}
