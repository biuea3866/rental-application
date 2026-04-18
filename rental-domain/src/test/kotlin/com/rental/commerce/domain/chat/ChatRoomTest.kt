package com.rental.commerce.domain.chat

import com.rental.commerce.domain.common.ChatAccessDeniedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ChatRoomTest : BehaviorSpec({

    // ─────────────────────────────────────────────────────────────
    // ChatRoom.create() — 정상 생성
    // ─────────────────────────────────────────────────────────────

    Given("ChatRoom.create() — 정상 생성") {

        When("rentalId, renterId, lenderId를 전달하면") {
            val chatRoom = ChatRoom.create(
                rentalId = 10L,
                renterId = 1L,
                lenderId = 2L,
            )

            Then("rentalId가 저장된다") {
                chatRoom.rentalId shouldBe 10L
            }

            Then("renterId가 저장된다") {
                chatRoom.renterId shouldBe 1L
            }

            Then("lenderId가 저장된다") {
                chatRoom.lenderId shouldBe 2L
            }

            Then("createdAt이 null이 아니다") {
                chatRoom.createdAt shouldNotBe null
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ChatRoom.isParticipant() — 참여자 여부 확인
    // ─────────────────────────────────────────────────────────────

    Given("ChatRoom.isParticipant() — 참여자 여부 확인") {

        val chatRoom = ChatRoom.create(rentalId = 10L, renterId = 1L, lenderId = 2L)

        When("renterId(1L)를 전달하면") {
            Then("true를 반환한다") {
                chatRoom.isParticipant(1L) shouldBe true
            }
        }

        When("lenderId(2L)를 전달하면") {
            Then("true를 반환한다") {
                chatRoom.isParticipant(2L) shouldBe true
            }
        }

        When("참여자가 아닌 userId(99L)를 전달하면") {
            Then("false를 반환한다") {
                chatRoom.isParticipant(99L) shouldBe false
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ChatRoom.verifyParticipant() — 접근 권한 검증
    // ─────────────────────────────────────────────────────────────

    Given("ChatRoom.verifyParticipant() — 접근 권한 검증") {

        val chatRoom = ChatRoom.create(rentalId = 10L, renterId = 1L, lenderId = 2L)

        When("renterId(1L)로 verifyParticipant를 호출하면") {
            Then("예외 없이 통과한다") {
                chatRoom.verifyParticipant(1L)
            }
        }

        When("lenderId(2L)로 verifyParticipant를 호출하면") {
            Then("예외 없이 통과한다") {
                chatRoom.verifyParticipant(2L)
            }
        }

        When("참여자가 아닌 userId(99L)로 verifyParticipant를 호출하면") {
            Then("ChatAccessDeniedException이 발생한다") {
                shouldThrow<ChatAccessDeniedException> {
                    chatRoom.verifyParticipant(99L)
                }
            }
        }
    }
})
