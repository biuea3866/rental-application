package com.rental.commerce.application.chat

import com.rental.commerce.domain.chat.ChatDomainService
import com.rental.commerce.domain.chat.ChatRoom
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GetChatRoomsUseCaseTest : BehaviorSpec({

    val chatDomainService = mockk<ChatDomainService>()
    val useCase = GetChatRoomsUseCase(chatDomainService)

    given("유효한 GetChatRoomsCommand가 주어졌을 때") {
        val command = GetChatRoomsCommand(userId = 10L)

        val chatRooms = listOf(
            ChatRoom.create(rentalId = 1L, renterId = 10L, lenderId = 20L),
            ChatRoom.create(rentalId = 2L, renterId = 10L, lenderId = 30L),
        )

        every { chatDomainService.getChatRooms(userId = 10L) } returns chatRooms

        `when`("execute()를 호출하면") {
            val result = useCase.execute(command)

            then("ChatDomainService.getChatRooms()가 호출된다") {
                verify(exactly = 1) { chatDomainService.getChatRooms(userId = 10L) }
            }

            then("채팅방 목록이 반환된다") {
                result.chatRooms.size shouldBe 2
                result.chatRooms[0].rentalId shouldBe 1L
                result.chatRooms[1].rentalId shouldBe 2L
            }
        }
    }

    given("참여 중인 채팅방이 없을 때") {
        val command = GetChatRoomsCommand(userId = 99L)

        every { chatDomainService.getChatRooms(userId = 99L) } returns emptyList()

        `when`("execute()를 호출하면") {
            val result = useCase.execute(command)

            then("빈 목록이 반환된다") {
                result.chatRooms shouldBe emptyList()
            }
        }
    }
})
