package com.rental.commerce.domain.product

import com.rental.commerce.domain.common.BaseEntity
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "product_price")
class ProductPrice(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_price_id")
    val productPriceId: Long = 0L,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "rental_unit", nullable = false, length = 20)
    val rentalUnit: RentalUnit,

    @Column(name = "price_amount", nullable = false)
    val priceAmount: Long,

) : BaseEntity() {

    init {
        validatePriceAmount(priceAmount)
    }

    private fun validatePriceAmount(amount: Long) {
        if (amount <= 0) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "가격은 0보다 커야 합니다")
        }
    }
}
