package com.rental.commerce.application.product

import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductImage
import com.rental.commerce.domain.product.ProductImageRepository
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.product.ProductSearchCondition
import com.rental.commerce.domain.product.ProductStatus
import com.rental.commerce.domain.product.RentalUnit
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class SearchProductUseCaseTest : BehaviorSpec({

    val productRepository = mockk<ProductRepository>()
    val productImageRepository = mockk<ProductImageRepository>()
    val useCase = SearchProductUseCase(
        productRepository = productRepository,
        productImageRepository = productImageRepository,
    )

    Given("상품 검색을 요청할 때") {

        When("키워드 없이 기본 조건으로 검색하면") {
            val condition = ProductSearchCondition()

            val products = listOf(
                Product(
                    productId = 1L,
                    userId = 10L,
                    name = "맥북 프로",
                    categoryCode = "ELECTRONICS",
                    status = ProductStatus.AVAILABLE,
                    depositAmount = 500000L,
                ),
                Product(
                    productId = 2L,
                    userId = 20L,
                    name = "아이패드 에어",
                    categoryCode = "ELECTRONICS",
                    status = ProductStatus.AVAILABLE,
                    depositAmount = 200000L,
                ),
            )

            val page = PageImpl(products, PageRequest.of(0, 20), 2L)

            every { productRepository.search(condition) } returns page
            every { productImageRepository.findByProductId(1L) } returns listOf(
                ProductImage(
                    productImageId = 100L,
                    productId = 1L,
                    objectKey = "products/thumb-001.jpg",
                    originalFilename = "front.jpg",
                    sortOrder = 1,
                ),
            )
            every { productImageRepository.findByProductId(2L) } returns emptyList()

            val result = useCase.execute(condition)

            Then("AVAILABLE 상태의 상품 목록이 반환된다") {
                result.totalElements shouldBe 2L
                result.content shouldHaveSize 2
            }

            Then("각 상품의 요약 정보가 포함된다") {
                result.content[0].productId shouldBe 1L
                result.content[0].name shouldBe "맥북 프로"
                result.content[0].categoryCode shouldBe "ELECTRONICS"
                result.content[0].status shouldBe ProductStatus.AVAILABLE
                result.content[0].depositAmount shouldBe 500000L
            }

            Then("첫 번째 이미지가 썸네일로 설정된다") {
                result.content[0].thumbnailUrl shouldBe "products/thumb-001.jpg"
                result.content[1].thumbnailUrl shouldBe null
            }
        }

        When("키워드로 검색하면") {
            val condition = ProductSearchCondition(keyword = "맥북")

            val products = listOf(
                Product(
                    productId = 1L,
                    userId = 10L,
                    name = "맥북 프로",
                    categoryCode = "ELECTRONICS",
                    status = ProductStatus.AVAILABLE,
                    depositAmount = 500000L,
                ),
            )

            val page = PageImpl(products, PageRequest.of(0, 20), 1L)

            every { productRepository.search(condition) } returns page
            every { productImageRepository.findByProductId(1L) } returns emptyList()

            val result = useCase.execute(condition)

            Then("키워드에 매칭되는 상품만 반환된다") {
                result.totalElements shouldBe 1L
                result.content shouldHaveSize 1
                result.content[0].name shouldBe "맥북 프로"
            }
        }

        When("카테고리 코드로 필터링하면") {
            val condition = ProductSearchCondition(categoryCode = "FASHION")

            val products = listOf(
                Product(
                    productId = 3L,
                    userId = 30L,
                    name = "구찌 가방",
                    categoryCode = "FASHION",
                    status = ProductStatus.AVAILABLE,
                    depositAmount = 1000000L,
                ),
            )

            val page = PageImpl(products, PageRequest.of(0, 20), 1L)

            every { productRepository.search(condition) } returns page
            every { productImageRepository.findByProductId(3L) } returns emptyList()

            val result = useCase.execute(condition)

            Then("해당 카테고리의 상품만 반환된다") {
                result.totalElements shouldBe 1L
                result.content[0].categoryCode shouldBe "FASHION"
            }
        }

        When("검색 결과가 없으면") {
            val condition = ProductSearchCondition(keyword = "존재하지않는상품")

            val page = PageImpl<Product>(emptyList(), PageRequest.of(0, 20), 0L)

            every { productRepository.search(condition) } returns page

            val result = useCase.execute(condition)

            Then("빈 페이지가 반환된다") {
                result.totalElements shouldBe 0L
                result.content shouldHaveSize 0
            }
        }

        When("페이지네이션을 적용하면") {
            val condition = ProductSearchCondition(page = 1, size = 10)

            val page = PageImpl<Product>(emptyList(), PageRequest.of(1, 10), 30L)

            every { productRepository.search(condition) } returns page

            val result = useCase.execute(condition)

            Then("요청한 페이지 정보가 반영된다") {
                result.totalElements shouldBe 30L
                result.number shouldBe 1
                result.size shouldBe 10
            }
        }

        When("가격 범위로 필터링하면") {
            val condition = ProductSearchCondition(
                minPrice = 10000L,
                maxPrice = 50000L,
                rentalUnit = RentalUnit.DAILY,
            )

            val products = listOf(
                Product(
                    productId = 4L,
                    userId = 40L,
                    name = "캠핑 텐트",
                    categoryCode = "OUTDOOR",
                    status = ProductStatus.AVAILABLE,
                    depositAmount = 100000L,
                ),
            )

            val page = PageImpl(products, PageRequest.of(0, 20), 1L)

            every { productRepository.search(condition) } returns page
            every { productImageRepository.findByProductId(4L) } returns emptyList()

            val result = useCase.execute(condition)

            Then("가격 범위에 맞는 상품만 반환된다") {
                result.totalElements shouldBe 1L
                result.content shouldHaveSize 1
            }

            Then("productRepository.search가 조건과 함께 호출된다") {
                verify { productRepository.search(condition) }
            }
        }
    }
})
