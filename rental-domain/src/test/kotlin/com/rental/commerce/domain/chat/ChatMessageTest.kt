package com.rental.commerce.domain.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ChatMessageTest : BehaviorSpec({

    // ─────────────────────────────────────────────────────────────
    // ChatMessage.create() — 정상 생성
    // ─────────────────────────────────────────────────────────────

    Given("ChatMessage.create() — 정상 생성") {

        When("chatRoomId, senderId, 유효한 content를 전달하면") {
            val message = ChatMessage.create(
                chatRoomId = 42L,
                senderId = 1L,
                content = "안녕하세요, 상품 상태는 어떤가요?",
            )

            Then("chatRoomId가 저장된다") {
                message.chatRoomId shouldBe 42L
            }

            Then("senderId가 저장된다") {
                message.senderId shouldBe 1L
            }

            Then("content가 저장된다") {
                message.content shouldBe "안녕하세요, 상품 상태는 어떤가요?"
            }

            Then("sentAt이 null이 아니다") {
                message.sentAt shouldNotBe null
            }
        }

        When("content가 정확히 1000자이면") {
            val maxContent = "가".repeat(1000)

            Then("정상 생성된다") {
                val message = ChatMessage.create(
                    chatRoomId = 42L,
                    senderId = 1L,
                    content = maxContent,
                )
                message.content shouldBe maxContent
            }
        }

        When("content가 1자이면") {
            Then("정상 생성된다") {
                val message = ChatMessage.create(
                    chatRoomId = 42L,
                    senderId = 1L,
                    content = "a",
                )
                message.content shouldBe "a"
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ChatMessage.create() — content 유효성 검증
    // ─────────────────────────────────────────────────────────────

    Given("ChatMessage.create() — content 유효성 검증") {

        When("content가 빈 문자열이면") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    ChatMessage.create(
                        chatRoomId = 42L,
                        senderId = 1L,
                        content = "",
                    )
                }
            }
        }

        When("content가 공백만으로 구성되면") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    ChatMessage.create(
                        chatRoomId = 42L,
                        senderId = 1L,
                        content = "   ",
                    )
                }
            }
        }

        When("content가 1001자(1000자 초과)이면") {
            val tooLongContent = "가".repeat(1001)

            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    ChatMessage.create(
                        chatRoomId = 42L,
                        senderId = 1L,
                        content = tooLongContent,
                    )
                }
            }
        }
    }
})
