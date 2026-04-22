package com.rental.commerce.domain.wishlist

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class WishlistDomainServiceTest : BehaviorSpec({

    val repo = mockk<WishlistRepository>()
    val service = WishlistDomainService(repo)

    beforeEach { clearMocks(repo) }

    Given("add") {
        When("이미 존재") {
            Then("WishlistAlreadyExistsException") {
                every { repo.existsByUserIdAndProductId(1L, 10L) } returns true
                shouldThrow<WishlistAlreadyExistsException> { service.add(1L, 10L) }
                verify(exactly = 0) { repo.save(any()) }
            }
        }
        When("신규 추가") {
            Then("save 호출 + 결과 반환") {
                every { repo.existsByUserIdAndProductId(1L, 20L) } returns false
                every { repo.save(any()) } answers { firstArg() }
                val saved = service.add(1L, 20L)
                saved.userId shouldBe 1L
                saved.productId shouldBe 20L
                verify(exactly = 1) { repo.save(any()) }
            }
        }
    }

    Given("remove") {
        When("삭제된 행 없음") {
            Then("WishlistNotFoundException") {
                every { repo.deleteByUserIdAndProductId(1L, 30L) } returns 0L
                shouldThrow<WishlistNotFoundException> { service.remove(1L, 30L) }
            }
        }
        When("정상 삭제") {
            Then("예외 없음") {
                every { repo.deleteByUserIdAndProductId(1L, 40L) } returns 1L
                service.remove(1L, 40L)
                verify(exactly = 1) { repo.deleteByUserIdAndProductId(1L, 40L) }
            }
        }
    }
})
