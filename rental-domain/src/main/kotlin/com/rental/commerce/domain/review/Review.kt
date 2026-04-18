package com.rental.commerce.domain.review

import com.rental.commerce.domain.common.BaseEntity
import com.rental.commerce.domain.common.ErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "review",
    uniqueConstraints = [UniqueConstraint(columnNames = ["rental_id"])],
)
class Review private constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L,

    @Column(name = "renter_id", nullable = false)
    val renterId: Long,

    @Column(name = "rental_id", nullable = false)
    val rentalId: Long,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    rating: Int,
    content: String,

) : BaseEntity() {

    @Column(name = "rating", nullable = false)
    var rating: Int = rating
        private set

    @Column(name = "content", nullable = false, length = 500)
    var content: String = content
        private set

    init {
        validateRating(rating)
        require(content.length in 10..500) {
            ErrorCode.REVIEW_CONTENT_TOO_SHORT.message
        }
    }

    fun validateRating(value: Int) {
        require(value in 1..5) {
            ErrorCode.REVIEW_INVALID_RATING.message
        }
    }

    companion object {
        fun create(
            renterId: Long,
            rentalId: Long,
            productId: Long,
            rating: Int,
            content: String,
        ): Review {
            return Review(
                renterId = renterId,
                rentalId = rentalId,
                productId = productId,
                rating = rating,
                content = content,
            )
        }
    }
}
