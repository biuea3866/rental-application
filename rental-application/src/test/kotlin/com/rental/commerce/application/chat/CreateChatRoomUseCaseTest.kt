package com.rental.commerce.application.chat

import com.rental.commerce.domain.chat.ChatDomainService
import com.rental.commerce.domain.chat.ChatRoom
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class CreateChatRoomUseCaseTest : BehaviorSpec({

    val chatDomainService = mockk<ChatDomainService>()
    val useCase = CreateChatRoomUseCase(chatDomainService)

    beforeEach {
        clearMocks(chatDomainService)
    }

    // ─────────────────────────────────────────────────────────────
    // execute() — 정상 채팅방 생성
    // ─────────────────────────────────────────────────────────────

    Given("유효한 CreateChatRoomCommand가 주어졌을 때") {

        When("execute()를 호출하면") {

            Then("ChatDomainService.getOrCreateChatRoom()이 호출되고 CreateChatRoomResult가 반환된다") {
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

                val command = CreateChatRoomCommand(
                    rentalId = 1L,
                    renterId = 10L,
                    lenderId = 20L,
                )
                val result = useCase.execute(command)

                verify(exactly = 1) {
                    chatDomainService.getOrCreateChatRoom(
                        rentalId = 1L,
                        renterId = 10L,
                        lenderId = 20L,
                    )
                }

                result.rentalId shouldBe 1L
                result.renterId shouldBe 10L
                result.lenderId shouldBe 20L
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 동일 rentalId로 채팅방 생성 시 기존 방 반환
    // ─────────────────────────────────────────────────────────────

    Given("동일 rentalId로 채팅방이 이미 존재할 때") {

        When("동일 rentalId(5L)로 execute()를 호출하면") {

            Then("기존 채팅방 정보가 반환되고 getOrCreateChatRoom()이 1회만 호출된다") {
                val existingChatRoom = ChatRoom.create(
                    rentalId = 5L,
                    renterId = 10L,
                    lenderId = 20L,
                )

                every {
                    chatDomainService.getOrCreateChatRoom(
                        rentalId = 5L,
                        renterId = 10L,
                        lenderId = 20L,
                    )
                } returns existingChatRoom

                val command = CreateChatRoomCommand(
                    rentalId = 5L,
                    renterId = 10L,
                    lenderId = 20L,
                )
                val result = useCase.execute(command)

                verify(exactly = 1) {
                    chatDomainService.getOrCreateChatRoom(
                        rentalId = 5L,
                        renterId = 10L,
                        lenderId = 20L,
                    )
                }

                result.rentalId shouldBe 5L
                result.renterId shouldBe 10L
                result.lenderId shouldBe 20L
            }
        }
    }
})
