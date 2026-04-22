package com.rental.commerce.infrastructure.wishlist.mysql

import com.rental.commerce.domain.wishlist.Wishlist
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface WishlistJpaRepository : JpaRepository<Wishlist, Long> {

    fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean

    fun findByUserIdAndProductId(userId: Long, productId: Long): Wishlist?

    fun deleteByUserIdAndProductId(userId: Long, productId: Long): Long

    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Wishlist>

    fun findAllByProductId(productId: Long): List<Wishlist>
}
