package com.rental.commerce.presentation.api.chat

import com.rental.commerce.application.chat.SendChatMessageCommand
import com.rental.commerce.application.chat.SendChatMessageResult
import com.rental.commerce.application.chat.SendChatMessageUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.ZonedDateTime
import org.springframework.messaging.simp.SimpMessagingTemplate

class ChatWebSocketControllerTest : BehaviorSpec({

    val sendChatMessageUseCase = mockk<SendChatMessageUseCase>()
    val messagingTemplate = mockk<SimpMessagingTemplate>(relaxed = true)

    val controller = ChatWebSocketController(
        sendChatMessageUseCase = sendChatMessageUseCase,
        messagingTemplate = messagingTemplate,
    )

    given("유효한 채팅 메시지 요청이 주어졌을 때") {
        val chatRoomId = 1L
        val request = SendMessageRequest(
            senderId = 10L,
            content = "안녕하세요",
        )

        val result = SendChatMessageResult(
            messageId = 100L,
            chatRoomId = chatRoomId,
            senderId = 10L,
            content = "안녕하세요",
            sentAt = ZonedDateTime.now(),
        )

        every {
            sendChatMessageUseCase.execute(
                SendChatMessageCommand(
                    chatRoomId = chatRoomId,
                    senderId = 10L,
                    content = "안녕하세요",
                )
            )
        } returns result

        `when`("handleMessage()를 호출하면") {
            controller.handleMessage(chatRoomId, request)

            then("SendChatMessageUseCase.execute()가 호출된다") {
                verify(exactly = 1) {
                    sendChatMessageUseCase.execute(
                        SendChatMessageCommand(
                            chatRoomId = chatRoomId,
                            senderId = 10L,
                            content = "안녕하세요",
                        )
                    )
                }
            }

            then("SimpMessagingTemplate으로 /topic/chat/{chatRoomId}에 브로드캐스트된다") {
                verify(exactly = 1) {
                    messagingTemplate.convertAndSend("/topic/chat/$chatRoomId", result)
                }
            }
        }
    }
})
