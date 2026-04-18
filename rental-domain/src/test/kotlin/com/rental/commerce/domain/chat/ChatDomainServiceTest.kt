package com.rental.commerce.domain.chat

import com.rental.commerce.domain.common.ChatAccessDeniedException
import com.rental.commerce.domain.common.ChatRoomNotFoundException
import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ChatDomainServiceTest : BehaviorSpec({

    fun newMocks(): Triple<ChatRoomRepository, ChatMessageRepository, ChatDomainService> {
        val chatRoomRepo = mockk<ChatRoomRepository>()
        val chatMessageRepo = mockk<ChatMessageRepository>()
        return Triple(chatRoomRepo, chatMessageRepo, ChatDomainService(chatRoomRepo, chatMessageRepo))
    }

    // ─────────────────────────────────────────────────────────────
    // getOrCreateChatRoom — 기존 채팅방 반환
    // ─────────────────────────────────────────────────────────────

    Given("getOrCreateChatRoom() — 이미 채팅방이 존재하는 경우") {

        When("동일 rentalId로 채팅방이 이미 존재하면") {
            val (chatRoomRepo, chatMessageRepo, service) = newMocks()
            val existingRoom = ChatRoom.create(rentalId = 10L, renterId = 1L, lenderId = 2L)

            every { chatRoomRepo.findByRentalId(10L) } returns existingRoom

            val result = service.getOrCreateChatRoom(rentalId = 10L, renterId = 1L, lenderId = 2L)

            Then("기존 채팅방을 반환한다") {
                result shouldBe existingRoom
            }

            Then("save()는 호출되지 않는다") {
                verify(exactly = 0) { chatRoomRepo.save(any()) }
            }
        }
    }

    Given("getOrCreateChatRoom() — 채팅방이 없는 경우") {

        When("rentalId에 해당하는 채팅방이 없으면") {
            val (chatRoomRepo, chatMessageRepo, service) = newMocks()
            val newRoom = ChatRoom.create(rentalId = 10L, renterId = 1L, lenderId = 2L)

            every { chatRoomRepo.findByRentalId(10L) } returns null
            every { chatRoomRepo.save(any()) } returns newRoom

            val result = service.getOrCreateChatRoom(rentalId = 10L, renterId = 1L, lenderId = 2L)

            Then("신규 채팅방이 저장된다") {
                verify(exactly = 1) { chatRoomRepo.save(any()) }
            }

            Then("반환된 채팅방의 rentalId가 일치한다") {
                result.rentalId shouldBe 10L
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // sendMessage — 참여자 검증
    // ─────────────────────────────────────────────────────────────

    Given("sendMessage() — 정상 메시지 전송") {

        When("채팅방 참여자(renterId=1)가 메시지를 보내면") {
            val (chatRoomRepo, chatMessageRepo, service) = newMocks()
            val chatRoom = ChatRoom.create(rentalId = 10L, renterId = 1L, lenderId = 2L)
            val savedMessage = ChatMessage.create(chatRoomId = 42L, senderId = 1L, content = "안녕하세요")

            every { chatRoomRepo.findById(42L) } returns chatRoom
            every { chatMessageRepo.save(any()) } returns savedMessage

            val result = service.sendMessage(chatRoomId = 42L, senderId = 1L, content = "안녕하세요")

            Then("메시지가 저장된다") {
                verify(exactly = 1) { chatMessageRepo.save(any()) }
            }

            Then("반환된 메시지의 senderId가 일치한다") {
                result.senderId shouldBe 1L
            }
        }
    }

    Given("sendMessage() — 비참여자 접근 거부") {

        When("채팅방에 참여하지 않은 userId(99L)가 메시지를 보내려 하면") {
            val (chatRoomRepo, chatMessageRepo, service) = newMocks()
            val chatRoom = ChatRoom.create(rentalId = 10L, renterId = 1L, lenderId = 2L)

            every { chatRoomRepo.findById(42L) } returns chatRoom

            Then("ChatAccessDeniedException이 발생한다") {
                shouldThrow<ChatAccessDeniedException> {
                    service.sendMessage(chatRoomId = 42L, senderId = 99L, content = "권한없음")
                }
            }

            Then("chatMessageRepository.save()는 호출되지 않는다") {
                verify(exactly = 0) { chatMessageRepo.save(any()) }
            }
        }
    }

    Given("sendMessage() — 채팅방 없음") {

        When("존재하지 않는 chatRoomId를 전달하면") {
            val (chatRoomRepo, chatMessageRepo, service) = newMocks()

            every { chatRoomRepo.findById(999L) } returns null

            Then("ChatRoomNotFoundException이 발생한다") {
                shouldThrow<ChatRoomNotFoundException> {
                    service.sendMessage(chatRoomId = 999L, senderId = 1L, content = "메시지")
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getChatRooms
    // ─────────────────────────────────────────────────────────────

    Given("getChatRooms() — userId 기준 채팅방 목록 조회") {

        When("userId=1인 사용자에게 채팅방이 2개 있으면") {
            val (chatRoomRepo, chatMessageRepo, service) = newMocks()
            val rooms = listOf(
                ChatRoom.create(rentalId = 10L, renterId = 1L, lenderId = 2L),
                ChatRoom.create(rentalId = 11L, renterId = 1L, lenderId = 3L),
            )

            every { chatRoomRepo.findByUserId(1L) } returns rooms

            val result = service.getChatRooms(1L)

            Then("2개의 채팅방이 반환된다") {
                result.size shouldBe 2
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getMessages — 참여자 검증 + 페이지네이션
    // ─────────────────────────────────────────────────────────────

    Given("getMessages() — 참여자가 메시지 목록 조회") {

        When("채팅방 참여자(lenderId=2)가 메시지 목록을 조회하면") {
            val (chatRoomRepo, chatMessageRepo, service) = newMocks()
            val chatRoom = ChatRoom.create(rentalId = 10L, renterId = 1L, lenderId = 2L)
            val messages = listOf(
                ChatMessage.create(chatRoomId = 42L, senderId = 1L, content = "첫 번째 메시지"),
                ChatMessage.create(chatRoomId = 42L, senderId = 2L, content = "두 번째 메시지"),
            )
            val pageQuery = PageQuery(page = 0, size = 20)

            every { chatRoomRepo.findById(42L) } returns chatRoom
            every { chatMessageRepo.findByChatRoomId(42L, pageQuery) } returns PageResult(
                content = messages,
                totalElements = 2L,
                totalPages = 1,
            )

            val result = service.getMessages(chatRoomId = 42L, userId = 2L, pageQuery = pageQuery)

            Then("2개의 메시지가 반환된다") {
                result.content.size shouldBe 2
                result.totalElements shouldBe 2L
            }
        }
    }

    Given("getMessages() — 비참여자 접근 거부") {

        When("채팅방에 참여하지 않은 userId(99L)가 메시지 목록을 조회하면") {
            val (chatRoomRepo, chatMessageRepo, service) = newMocks()
            val chatRoom = ChatRoom.create(rentalId = 10L, renterId = 1L, lenderId = 2L)
            val pageQuery = PageQuery(page = 0, size = 20)

            every { chatRoomRepo.findById(42L) } returns chatRoom

            Then("ChatAccessDeniedException이 발생한다") {
                shouldThrow<ChatAccessDeniedException> {
                    service.getMessages(chatRoomId = 42L, userId = 99L, pageQuery = pageQuery)
                }
            }

            Then("chatMessageRepository.findByChatRoomId()는 호출되지 않는다") {
                verify(exactly = 0) { chatMessageRepo.findByChatRoomId(any(), any()) }
            }
        }
    }
})
