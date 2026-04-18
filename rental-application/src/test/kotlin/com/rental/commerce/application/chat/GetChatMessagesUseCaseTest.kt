package com.rental.commerce.application.chat

import com.rental.commerce.domain.chat.ChatDomainService
import com.rental.commerce.domain.chat.ChatMessage
import com.rental.commerce.domain.common.ChatAccessDeniedException
import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GetChatMessagesUseCaseTest : BehaviorSpec({

    val chatDomainService = mockk<ChatDomainService>()
    val useCase = GetChatMessagesUseCase(chatDomainService)

    given("참여자가 메시지 이력을 요청할 때") {
        val command = GetChatMessagesCommand(
            chatRoomId = 1L,
            userId = 10L,
            page = 0,
            size = 20,
        )

        val messages = listOf(
            ChatMessage.create(chatRoomId = 1L, senderId = 10L, content = "안녕하세요"),
            ChatMessage.create(chatRoomId = 1L, senderId = 20L, content = "반갑습니다"),
        )

        every {
            chatDomainService.getMessages(
                chatRoomId = 1L,
                userId = 10L,
                pageQuery = PageQuery(page = 0, size = 20),
            )
        } returns PageResult(content = messages, totalElements = 2L, totalPages = 1)

        `when`("execute()를 호출하면") {
            val result = useCase.execute(command)

            then("ChatDomainService.getMessages()가 호출된다") {
                verify(exactly = 1) {
                    chatDomainService.getMessages(
                        chatRoomId = 1L,
                        userId = 10L,
                        pageQuery = PageQuery(page = 0, size = 20),
                    )
                }
            }

            then("메시지 목록이 반환된다") {
                result.messages.content.size shouldBe 2
                result.messages.totalElements shouldBe 2L
                result.messages.content[0].content shouldBe "안녕하세요"
            }
        }
    }

    given("참여자가 아닌 사용자가 메시지 이력을 요청할 때") {
        val command = GetChatMessagesCommand(
            chatRoomId = 1L,
            userId = 99L,
            page = 0,
            size = 20,
        )

        every {
            chatDomainService.getMessages(
                chatRoomId = 1L,
                userId = 99L,
                pageQuery = PageQuery(page = 0, size = 20),
            )
        } throws ChatAccessDeniedException("채팅방 접근 권한이 없습니다.")

        `when`("execute()를 호출하면") {
            then("ChatAccessDeniedException이 발생한다") {
                shouldThrow<ChatAccessDeniedException> {
                    useCase.execute(command)
                }
            }
        }
    }
})
