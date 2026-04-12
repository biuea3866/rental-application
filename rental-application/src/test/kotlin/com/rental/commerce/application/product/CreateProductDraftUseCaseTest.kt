package com.rental.commerce.application.product

import com.rental.commerce.domain.product.Product
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.product.ProductStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class CreateProductDraftUseCaseTest : BehaviorSpec({

    val productRepository = mockk<ProductRepository>()
    val useCase = CreateProductDraftUseCase(productRepository)

    Given("DRAFT 상품 생성을 요청할 때") {

        When("이름과 카테고리 코드 없이 요청하면") {
            val command = CreateProductDraftCommand(
                userId = 1L,
            )

            val productSlot = slot<Product>()
            every { productRepository.save(capture(productSlot)) } answers {
                productSlot.captured
            }

            val result = useCase.execute(command)

            Then("DRAFT 상태의 상품이 생성된다") {
                result.status shouldBe ProductStatus.DRAFT
            }

            Then("currentDraftStep이 1로 설정된다") {
                result.currentDraftStep shouldBe 1
            }

            Then("name은 null이다") {
                result.name shouldBe null
            }

            Then("categoryCode는 null이다") {
                result.categoryCode shouldBe null
            }

            Then("ProductRepository.save가 정확히 한 번 호출된다") {
                verify(exactly = 1) { productRepository.save(any()) }
            }
        }

        When("이름과 카테고리 코드를 포함하여 요청하면") {
            val command = CreateProductDraftCommand(
                userId = 2L,
                name = "맥북 프로 16인치",
                categoryCode = "ELECTRONICS",
            )

            val productSlot = slot<Product>()
            every { productRepository.save(capture(productSlot)) } answers {
                productSlot.captured
            }

            val result = useCase.execute(command)

            Then("DRAFT 상태의 상품이 생성된다") {
                result.status shouldBe ProductStatus.DRAFT
            }

            Then("이름이 설정된다") {
                result.name shouldBe "맥북 프로 16인치"
            }

            Then("카테고리 코드가 설정된다") {
                result.categoryCode shouldBe "ELECTRONICS"
            }

            Then("currentDraftStep이 1로 설정된다") {
                result.currentDraftStep shouldBe 1
            }
        }
    }
})
