package com.rental.commerce.application.chat

import com.rental.commerce.domain.chat.ChatDomainService
import com.rental.commerce.domain.chat.ChatRoom
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class CreateChatRoomUseCaseTest : BehaviorSpec({

    val chatDomainService = mockk<ChatDomainService>()
    val useCase = CreateChatRoomUseCase(chatDomainService)

    given("유효한 CreateChatRoomCommand가 주어졌을 때") {
        val command = CreateChatRoomCommand(
            rentalId = 1L,
            renterId = 10L,
            lenderId = 20L,
        )

        val chatRoom = ChatRoom.create(
            rentalId = 1L,
            renterId = 10L,
            lenderId = 20L,
        )

        every {
            chatDomainService.getOrCreateChatRoom(
                rentalId = 1L,
                renterId = 10L,
                lenderId = 20L,
            )
        } returns chatRoom

        `when`("execute()를 호출하면") {
            val result = useCase.execute(command)

            then("ChatDomainService.getOrCreateChatRoom()이 호출된다") {
                verify(exactly = 1) {
                    chatDomainService.getOrCreateChatRoom(
                        rentalId = 1L,
                        renterId = 10L,
                        lenderId = 20L,
                    )
                }
            }

            then("CreateChatRoomResult가 반환된다") {
                result.rentalId shouldBe 1L
                result.renterId shouldBe 10L
                result.lenderId shouldBe 20L
            }
        }
    }
})
