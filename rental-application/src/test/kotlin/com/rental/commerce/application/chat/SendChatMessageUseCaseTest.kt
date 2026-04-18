package com.rental.commerce.application.chat

import com.rental.commerce.domain.chat.ChatDomainService
import com.rental.commerce.domain.chat.ChatMessage
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime

class SendChatMessageUseCaseTest : BehaviorSpec({

    val chatDomainService = mockk<ChatDomainService>()
    val useCase = SendChatMessageUseCase(chatDomainService)

    given("유효한 SendChatMessageCommand가 주어졌을 때") {
        val command = SendChatMessageCommand(
            chatRoomId = 1L,
            senderId = 10L,
            content = "안녕하세요",
        )

        val savedMessage = ChatMessage.create(
            chatRoomId = 1L,
            senderId = 10L,
            content = "안녕하세요",
        )

        every {
            chatDomainService.sendMessage(
                chatRoomId = 1L,
                senderId = 10L,
                content = "안녕하세요",
            )
        } returns savedMessage

        `when`("execute()를 호출하면") {
            val result = useCase.execute(command)

            then("ChatDomainService.sendMessage()가 호출된다") {
                verify(exactly = 1) {
                    chatDomainService.sendMessage(
                        chatRoomId = 1L,
                        senderId = 10L,
                        content = "안녕하세요",
                    )
                }
            }

            then("SendChatMessageResult가 반환된다") {
                result.chatRoomId shouldBe 1L
                result.senderId shouldBe 10L
                result.content shouldBe "안녕하세요"
            }
        }
    }
})
