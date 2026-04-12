package com.rental.commerce.domain.product

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.product.event.ProductApprovedEvent
import com.rental.commerce.domain.product.event.ProductRejectedEvent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ProductTest : BehaviorSpec({

    fun createDraftProduct(
        userId: Long = 1L,
    ): Product {
        return Product(userId = userId)
    }

    fun createFullDraftProduct(
        userId: Long = 1L,
    ): Product {
        val product = Product(userId = userId)
        product.updateDraft(
            step = 1,
            name = "테스트 상품",
            description = "테스트 설명",
            categoryCode = "ELECTRONICS",
            condition = ProductCondition.GOOD,
            depositAmount = 50_000L,
        )
        return product
    }

    Given("updateDraft - DRAFT 상태에서 임시저장 업데이트") {

        When("step 1에서 기본 정보를 저장하면") {
            val product = createDraftProduct()
            product.updateDraft(
                step = 1,
                name = "테스트 상품",
                description = "테스트 설명",
                categoryCode = "ELECTRONICS",
                condition = ProductCondition.LIKE_NEW,
                depositAmount = 100_000L,
            )

            Then("상품 정보가 업데이트된다") {
                product.name shouldBe "테스트 상품"
                product.description shouldBe "테스트 설명"
                product.categoryCode shouldBe "ELECTRONICS"
                product.condition shouldBe ProductCondition.LIKE_NEW
                product.depositAmount shouldBe 100_000L
                product.currentDraftStep shouldBe 1
                product.status shouldBe ProductStatus.DRAFT
            }
        }

        When("DRAFT가 아닌 상태에서 updateDraft를 호출하면") {
            val product = createFullDraftProduct()
            product.submit()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    product.updateDraft(
                        step = 1,
                        name = "변경",
                        description = null,
                        categoryCode = null,
                        condition = null,
                        depositAmount = null,
                    )
                }
            }
        }
    }

    Given("submit - DRAFT에서 UNDER_REVIEW로 전이") {

        When("필수 필드가 모두 채워진 상태에서 submit하면") {
            val product = createFullDraftProduct()
            product.submit()

            Then("상태가 UNDER_REVIEW로 변경된다") {
                product.status shouldBe ProductStatus.UNDER_REVIEW
            }

            Then("currentDraftStep이 null로 초기화된다") {
                product.currentDraftStep shouldBe null
            }
        }

        When("name이 없는 상태에서 submit하면") {
            val product = createDraftProduct()
            product.updateDraft(
                step = 1,
                name = null,
                description = "설명",
                categoryCode = "ELECTRONICS",
                condition = ProductCondition.GOOD,
                depositAmount = 50_000L,
            )

            Then("BusinessException(INVALID_INPUT)이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    product.submit()
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }

        When("description이 없는 상태에서 submit하면") {
            val product = createDraftProduct()
            product.updateDraft(
                step = 1,
                name = "상품명",
                description = null,
                categoryCode = "ELECTRONICS",
                condition = ProductCondition.GOOD,
                depositAmount = 50_000L,
            )

            Then("BusinessException(INVALID_INPUT)이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    product.submit()
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }

        When("categoryCode가 없는 상태에서 submit하면") {
            val product = createDraftProduct()
            product.updateDraft(
                step = 1,
                name = "상품명",
                description = "설명",
                categoryCode = null,
                condition = ProductCondition.GOOD,
                depositAmount = 50_000L,
            )

            Then("BusinessException(INVALID_INPUT)이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    product.submit()
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }

        When("condition이 없는 상태에서 submit하면") {
            val product = createDraftProduct()
            product.updateDraft(
                step = 1,
                name = "상품명",
                description = "설명",
                categoryCode = "ELECTRONICS",
                condition = null,
                depositAmount = 50_000L,
            )

            Then("BusinessException(INVALID_INPUT)이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    product.submit()
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }

        When("depositAmount가 없는 상태에서 submit하면") {
            val product = createDraftProduct()
            product.updateDraft(
                step = 1,
                name = "상품명",
                description = "설명",
                categoryCode = "ELECTRONICS",
                condition = ProductCondition.GOOD,
                depositAmount = null,
            )

            Then("BusinessException(INVALID_INPUT)이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    product.submit()
                }
                exception.errorCode shouldBe ErrorCode.INVALID_INPUT
            }
        }

        When("DRAFT가 아닌 상태에서 submit하면") {
            val product = createFullDraftProduct()
            product.submit()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    product.submit()
                }
            }
        }
    }

    Given("approve - UNDER_REVIEW에서 APPROVED로 전이") {

        When("UNDER_REVIEW 상태에서 approve하면") {
            val product = createFullDraftProduct()
            product.submit()
            product.approve()

            Then("상태가 APPROVED로 변경된다") {
                product.status shouldBe ProductStatus.APPROVED
            }

            Then("ProductApprovedEvent가 발행된다") {
                val events = product.pullEvents()
                events shouldHaveSize 1
                events.first().shouldBeInstanceOf<ProductApprovedEvent>()
            }
        }

        When("approve 후 pullEvents를 두 번 호출하면") {
            val product = createFullDraftProduct()
            product.submit()
            product.approve()
            product.pullEvents()
            val secondPull = product.pullEvents()

            Then("두 번째 호출에서는 빈 리스트가 반환된다") {
                secondPull shouldHaveSize 0
            }
        }

        When("UNDER_REVIEW가 아닌 상태에서 approve하면") {
            val product = createDraftProduct()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    product.approve()
                }
            }
        }
    }

    Given("reject - UNDER_REVIEW에서 REJECTED로 전이") {

        When("UNDER_REVIEW 상태에서 reject하면") {
            val product = createFullDraftProduct()
            product.submit()
            val reason = "상품 설명이 부족합니다"
            product.reject(reason)

            Then("상태가 REJECTED로 변경된다") {
                product.status shouldBe ProductStatus.REJECTED
            }

            Then("rejectReason이 설정된다") {
                product.rejectReason shouldBe reason
            }

            Then("ProductRejectedEvent가 발행된다") {
                val events = product.pullEvents()
                events shouldHaveSize 1
                val event = events.first()
                event.shouldBeInstanceOf<ProductRejectedEvent>()
                (event as ProductRejectedEvent).reason shouldBe reason
            }
        }

        When("UNDER_REVIEW가 아닌 상태에서 reject하면") {
            val product = createDraftProduct()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    product.reject("사유")
                }
            }
        }
    }

    Given("makeAvailable - APPROVED에서 AVAILABLE로 전이") {

        When("APPROVED 상태에서 makeAvailable하면") {
            val product = createFullDraftProduct()
            product.submit()
            product.approve()
            product.pullEvents() // clear
            product.makeAvailable()

            Then("상태가 AVAILABLE로 변경된다") {
                product.status shouldBe ProductStatus.AVAILABLE
            }
        }

        When("APPROVED가 아닌 상태에서 makeAvailable하면") {
            val product = createDraftProduct()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    product.makeAvailable()
                }
            }
        }
    }

    Given("revertToDraft - REJECTED에서 DRAFT로 전이") {

        When("REJECTED 상태에서 revertToDraft하면") {
            val product = createFullDraftProduct()
            product.submit()
            product.reject("사유")
            product.pullEvents() // clear
            product.revertToDraft()

            Then("상태가 DRAFT로 변경된다") {
                product.status shouldBe ProductStatus.DRAFT
            }

            Then("rejectReason이 null로 초기화된다") {
                product.rejectReason shouldBe null
            }
        }

        When("REJECTED가 아닌 상태에서 revertToDraft하면") {
            val product = createDraftProduct()

            Then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    product.revertToDraft()
                }
            }
        }
    }

    Given("delete - DRAFT/REJECTED 상태에서 삭제") {

        When("DRAFT 상태에서 delete하면") {
            val product = createDraftProduct()
            product.delete()

            Then("상태가 DELETED로 변경된다") {
                product.status shouldBe ProductStatus.DELETED
            }

            Then("deletedAt이 설정된다") {
                product.deletedAt shouldNotBe null
            }
        }

        When("REJECTED 상태에서 delete하면") {
            val product = createFullDraftProduct()
            product.submit()
            product.reject("사유")
            product.pullEvents()
            product.delete()

            Then("상태가 DELETED로 변경된다") {
                product.status shouldBe ProductStatus.DELETED
            }

            Then("deletedAt이 설정된다") {
                product.deletedAt shouldNotBe null
            }
        }

        When("UNDER_REVIEW 상태에서 delete하면") {
            val product = createFullDraftProduct()
            product.submit()

            Then("BusinessException(PRODUCT_NOT_DELETABLE)이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    product.delete()
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_DELETABLE
            }
        }

        When("APPROVED 상태에서 delete하면") {
            val product = createFullDraftProduct()
            product.submit()
            product.approve()
            product.pullEvents()

            Then("BusinessException(PRODUCT_NOT_DELETABLE)이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    product.delete()
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_DELETABLE
            }
        }

        When("AVAILABLE 상태에서 delete하면") {
            val product = createFullDraftProduct()
            product.submit()
            product.approve()
            product.pullEvents()
            product.makeAvailable()

            Then("BusinessException(PRODUCT_NOT_DELETABLE)이 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    product.delete()
                }
                exception.errorCode shouldBe ErrorCode.PRODUCT_NOT_DELETABLE
            }
        }
    }
})
