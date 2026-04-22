package com.rental.commerce.domain.wishlist

import com.rental.commerce.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 위시리스트 항목 (PRD-004 §4.4).
 *
 * 사용자-상품 1:N. UNIQUE(user_id, product_id) — V22 마이그레이션.
 */
@Entity
@Table(
    name = "wishlist",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "product_id"])],
)
class Wishlist private constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

) : BaseEntity() {

    companion object {
        fun create(userId: Long, productId: Long): Wishlist =
            Wishlist(userId = userId, productId = productId)
    }
}
