package com.rental.commerce.infrastructure.kafka

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class RentalTopicsTest : BehaviorSpec({

    given("RentalTopics 상수") {

        `when`("STATUS_CHANGED 값을 확인할 때") {

            then("'event.rental.status-changed' 와 일치해야 한다") {
                RentalTopics.STATUS_CHANGED shouldBe "event.rental.status-changed"
            }
        }

        `when`("STATUS_CHANGED 가 event.rental. 접두사를 가질 때") {

            then("접두사가 'event.rental.' 이어야 한다") {
                RentalTopics.STATUS_CHANGED.startsWith("event.rental.") shouldBe true
            }
        }
    }
})
