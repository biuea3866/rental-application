package com.rental.commerce.domain.product

import com.rental.commerce.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "product_image")
class ProductImage(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_image_id")
    val productImageId: Long = 0L,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "object_key", nullable = false, length = 500)
    val objectKey: String,

    @Column(name = "original_filename", nullable = false, length = 255)
    val originalFilename: String,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Short,

) : BaseEntity() {

    fun changeSortOrder(newSortOrder: Short) {
        this.sortOrder = newSortOrder
    }
}
