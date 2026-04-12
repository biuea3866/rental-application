package com.rental.commerce.application.product

import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductCondition
import com.rental.commerce.domain.product.ProductDomainService
import com.rental.commerce.domain.product.ProductStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class GetMyProductsUseCaseTest : BehaviorSpec({

    val productDomainService = mockk<ProductDomainService>()
    val useCase = GetMyProductsUseCase(
        productDomainService = productDomainService,
    )

    Given("내 상품 목록 조회를 요청할 때") {

        When("등록한 상품이 있으면") {
            val userId = 1L
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))

            val products = listOf(
                Product(
                    productId = 1L,
                    userId = userId,
                    name = "맥북 프로",
                    categoryCode = "ELECTRONICS",
                    condition = ProductCondition.LIKE_NEW,
                    status = ProductStatus.DRAFT,
                    depositAmount = 500_000L,
                ),
                Product(
                    productId = 2L,
                    userId = userId,
                    name = "아이패드",
                    categoryCode = "ELECTRONICS",
                    condition = ProductCondition.GOOD,
                    status = ProductStatus.AVAILABLE,
                    depositAmount = 200_000L,
                ),
            )

            every {
                productDomainService.getMyProducts(
                    userId = userId,
                    pageable = pageable,
                )
            } returns PageImpl(products, pageable, 2L)

            val result = useCase.execute(userId = userId, page = 0, size = 20)

            Then("상품 목록이 반환된다") {
                result.content shouldHaveSize 2
            }

            Then("첫 번째 상품 정보가 올바르게 매핑된다") {
                val first = result.content[0]
                first.id shouldBe 1L
                first.name shouldBe "맥북 프로"
                first.categoryCode shouldBe "ELECTRONICS"
                first.condition shouldBe ProductCondition.LIKE_NEW
                first.status shouldBe ProductStatus.DRAFT
                first.depositAmount shouldBe 500_000L
            }

            Then("두 번째 상품 정보가 올바르게 매핑된다") {
                val second = result.content[1]
                second.id shouldBe 2L
                second.name shouldBe "아이패드"
                second.status shouldBe ProductStatus.AVAILABLE
            }

            Then("페이지 정보가 올바르다") {
                result.totalElements shouldBe 2L
                result.totalPages shouldBe 1
                result.number shouldBe 0
            }
        }

        When("등록한 상품이 없으면") {
            val userId = 2L
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))

            every {
                productDomainService.getMyProducts(
                    userId = userId,
                    pageable = pageable,
                )
            } returns PageImpl(emptyList(), pageable, 0L)

            val result = useCase.execute(userId = userId, page = 0, size = 20)

            Then("빈 목록이 반환된다") {
                result.content shouldHaveSize 0
                result.totalElements shouldBe 0L
            }
        }

        When("두 번째 페이지를 요청하면") {
            val userId = 3L
            val pageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createdAt"))

            val products = listOf(
                Product(
                    productId = 11L,
                    userId = userId,
                    name = "카메라",
                    categoryCode = "ELECTRONICS",
                    status = ProductStatus.DRAFT,
                ),
            )

            every {
                productDomainService.getMyProducts(
                    userId = userId,
                    pageable = pageable,
                )
            } returns PageImpl(products, pageable, 11L)

            val result = useCase.execute(userId = userId, page = 1, size = 10)

            Then("두 번째 페이지 결과가 반환된다") {
                result.content shouldHaveSize 1
                result.number shouldBe 1
                result.totalElements shouldBe 11L
                result.totalPages shouldBe 2
            }
        }
    }
})
