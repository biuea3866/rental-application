package com.rental.commerce.presentation.api.chat

import com.rental.commerce.application.chat.CreateChatRoomResult
import com.rental.commerce.application.chat.CreateChatRoomUseCase
import com.rental.commerce.application.chat.GetChatMessagesResult
import com.rental.commerce.application.chat.GetChatMessagesUseCase
import com.rental.commerce.application.chat.GetChatRoomsResult
import com.rental.commerce.application.chat.GetChatRoomsUseCase
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ChatAccessDeniedException
import com.rental.commerce.domain.common.ChatRoomNotFoundException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.presentation.api.common.AuthenticatedRequestWrapper
import com.rental.commerce.presentation.api.common.GlobalExceptionHandler
import com.rental.commerce.presentation.api.common.MemberIdArgumentResolver
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ChatApiControllerTest : BehaviorSpec({

    val createChatRoomUseCase = mockk<CreateChatRoomUseCase>()
    val getChatRoomsUseCase = mockk<GetChatRoomsUseCase>()
    val getChatMessagesUseCase = mockk<GetChatMessagesUseCase>()

    val controller = ChatApiController(
        createChatRoomUseCase = createChatRoomUseCase,
        getChatRoomsUseCase = getChatRoomsUseCase,
        getChatMessagesUseCase = getChatMessagesUseCase,
    )

    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setCustomArgumentResolvers(MemberIdArgumentResolver())
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    beforeEach {
        clearMocks(createChatRoomUseCase, getChatRoomsUseCase, getChatMessagesUseCase)
    }

    // ──────────────────────────────────────────────────────────
    // POST /api/v1/chat-rooms — 채팅방 생성/조회
    // ──────────────────────────────────────────────────────────
    Given("POST /api/v1/chat-rooms") {

        When("유효한 채팅방 생성 요청을 보내면") {
            Then("201 Created와 채팅방 정보가 반환된다") {
                val now = ZonedDateTime.now()
                val response = CreateChatRoomResult(
                    chatRoomId = 1L,
                    rentalId = 100L,
                    renterId = 10L,
                    lenderId = 20L,
                    createdAt = now,
                )

                every { createChatRoomUseCase.execute(any()) } returns response

                val result = mockMvc.post("/api/v1/chat-rooms") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """
                        {
                          "rentalId": 100,
                          "renterId": 10,
                          "lenderId": 20
                        }
                    """.trimIndent()
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "10")
                }

                result.andExpect {
                    status { isCreated() }
                    jsonPath("$.chatRoomId") { value(1) }
                    jsonPath("$.rentalId") { value(100) }
                    jsonPath("$.renterId") { value(10) }
                    jsonPath("$.lenderId") { value(20) }
                }

                verify { createChatRoomUseCase.execute(any()) }
            }
        }

        When("rentalId가 누락된 경우") {
            Then("400 Bad Request가 반환된다") {
                val result = mockMvc.post("/api/v1/chat-rooms") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"renterId": 10, "lenderId": 20}"""
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "10")
                }

                result.andExpect {
                    status { isBadRequest() }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // GET /api/v1/chat-rooms — 내 채팅방 목록
    // ──────────────────────────────────────────────────────────
    Given("GET /api/v1/chat-rooms") {

        When("채팅방 목록 조회 요청을 보내면") {
            Then("200 OK와 채팅방 목록이 반환된다") {
                val now = ZonedDateTime.now()
                val chatRoomSummaries = listOf(
                    GetChatRoomsResult.ChatRoomSummary(
                        chatRoomId = 1L,
                        rentalId = 100L,
                        renterId = 10L,
                        lenderId = 20L,
                        createdAt = now,
                    ),
                    GetChatRoomsResult.ChatRoomSummary(
                        chatRoomId = 2L,
                        rentalId = 200L,
                        renterId = 10L,
                        lenderId = 30L,
                        createdAt = now,
                    ),
                )
                val response = GetChatRoomsResult(chatRooms = chatRoomSummaries)

                every { getChatRoomsUseCase.execute(any()) } returns response

                val result = mockMvc.get("/api/v1/chat-rooms") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "10")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.chatRooms.length()") { value(2) }
                    jsonPath("$.chatRooms[0].chatRoomId") { value(1) }
                    jsonPath("$.chatRooms[0].rentalId") { value(100) }
                    jsonPath("$.chatRooms[1].chatRoomId") { value(2) }
                }

                verify { getChatRoomsUseCase.execute(any()) }
            }
        }

        When("채팅방이 없는 경우") {
            Then("200 OK와 빈 목록이 반환된다") {
                every { getChatRoomsUseCase.execute(any()) } returns GetChatRoomsResult(chatRooms = emptyList())

                val result = mockMvc.get("/api/v1/chat-rooms") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "99")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.chatRooms.length()") { value(0) }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // GET /api/v1/chat-rooms/{id}/messages — 메시지 이력
    // ──────────────────────────────────────────────────────────
    Given("GET /api/v1/chat-rooms/{id}/messages") {

        When("참여자가 메시지 이력을 조회하면") {
            Then("200 OK와 메시지 목록이 반환된다") {
                val now = ZonedDateTime.now()
                val messageSummaries = listOf(
                    GetChatMessagesResult.MessageSummary(
                        messageId = 1L,
                        chatRoomId = 1L,
                        senderId = 10L,
                        content = "안녕하세요",
                        sentAt = now,
                    ),
                )
                val response = GetChatMessagesResult(
                    messages = PageResult(
                        content = messageSummaries,
                        totalElements = 1L,
                        totalPages = 1,
                    )
                )

                every { getChatMessagesUseCase.execute(any()) } returns response

                val result = mockMvc.get("/api/v1/chat-rooms/1/messages") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "10")
                }

                result.andExpect {
                    status { isOk() }
                    jsonPath("$.messages.content.length()") { value(1) }
                    jsonPath("$.messages.content[0].messageId") { value(1) }
                    jsonPath("$.messages.content[0].senderId") { value(10) }
                    jsonPath("$.messages.content[0].content") { value("안녕하세요") }
                    jsonPath("$.messages.totalElements") { value(1) }
                }

                verify { getChatMessagesUseCase.execute(any()) }
            }
        }

        When("참여자가 아닌 사용자가 메시지 이력을 조회하면") {
            Then("403 CHAT_ACCESS_DENIED 에러가 반환된다") {
                every { getChatMessagesUseCase.execute(any()) } throws ChatAccessDeniedException()

                val result = mockMvc.get("/api/v1/chat-rooms/1/messages") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "99")
                }

                result.andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("CHAT_ACCESS_DENIED") }
                }
            }
        }

        When("채팅방이 존재하지 않는 경우") {
            Then("404 CHAT_ROOM_NOT_FOUND 에러가 반환된다") {
                every { getChatMessagesUseCase.execute(any()) } throws ChatRoomNotFoundException()

                val result = mockMvc.get("/api/v1/chat-rooms/9999/messages") {
                    header(AuthenticatedRequestWrapper.HEADER_USER_ID, "10")
                }

                result.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("CHAT_ROOM_NOT_FOUND") }
                }
            }
        }
    }
})
