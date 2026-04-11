package com.rental.commerce.domain.product

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ProductImageTest : BehaviorSpec({

    Given("ProductImage 생성") {

        When("유효한 정보로 생성하면") {
            val image = ProductImage(
                productId = 1L,
                objectKey = "products/1/image-001.jpg",
                originalFilename = "my-product-photo.jpg",
                sortOrder = 0,
            )

            Then("정상적으로 생성된다") {
                image.productId shouldBe 1L
                image.objectKey shouldBe "products/1/image-001.jpg"
                image.originalFilename shouldBe "my-product-photo.jpg"
                image.sortOrder shouldBe 0.toShort()
            }
        }

        When("여러 이미지를 서로 다른 정렬 순서로 생성하면") {
            val image1 = ProductImage(
                productId = 1L,
                objectKey = "products/1/image-001.jpg",
                originalFilename = "photo1.jpg",
                sortOrder = 0,
            )
            val image2 = ProductImage(
                productId = 1L,
                objectKey = "products/1/image-002.jpg",
                originalFilename = "photo2.jpg",
                sortOrder = 1,
            )
            val image3 = ProductImage(
                productId = 1L,
                objectKey = "products/1/image-003.jpg",
                originalFilename = "photo3.jpg",
                sortOrder = 2,
            )

            Then("정렬 순서가 올바르게 설정된다") {
                image1.sortOrder shouldBe 0.toShort()
                image2.sortOrder shouldBe 1.toShort()
                image3.sortOrder shouldBe 2.toShort()
            }
        }
    }

    Given("ProductImage 정렬 순서 변경") {

        When("changeSortOrder로 정렬 순서를 변경하면") {
            val image = ProductImage(
                productId = 1L,
                objectKey = "products/1/image-001.jpg",
                originalFilename = "photo.jpg",
                sortOrder = 0,
            )
            image.changeSortOrder(3)

            Then("새로운 정렬 순서가 반영된다") {
                image.sortOrder shouldBe 3.toShort()
            }
        }
    }
})
