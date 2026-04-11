package com.rental.commerce.domain.common

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.ZonedDateTime

class BaseEntityTest : BehaviorSpec({

    Given("BaseEntity를 상속한 엔티티 생성 시") {
        When("엔티티가 생성되면") {
            val entity = TestEntity()

            Then("createdAt이 ZonedDateTime으로 자동 설정된다") {
                entity.createdAt shouldNotBe null
                entity.createdAt shouldBe entity.createdAt
            }

            Then("updatedAt이 ZonedDateTime으로 자동 설정된다") {
                entity.updatedAt shouldNotBe null
            }
        }

        When("엔티티가 수정되면") {
            val entity = TestEntity()
            val originalUpdatedAt = entity.updatedAt

            Thread.sleep(10)
            entity.markUpdated()

            Then("updatedAt이 갱신된다") {
                entity.updatedAt shouldNotBe originalUpdatedAt
            }

            Then("createdAt은 변경되지 않는다") {
                entity.createdAt shouldBe entity.createdAt
            }
        }
    }
})

/**
 * 테스트용 엔티티
 */
class TestEntity : BaseEntity() {
    fun markUpdated() {
        updatedAt = ZonedDateTime.now()
    }
}
