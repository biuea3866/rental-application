package com.rental.commerce.presentation.api.chat

import com.rental.commerce.application.chat.SendChatMessageCommand
import com.rental.commerce.application.chat.SendChatMessageResult
import com.rental.commerce.application.chat.SendChatMessageUseCase
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

/**
 * ChatWebSocketController
 *
 * STOMP WebSocket 채팅 컨트롤러.
 * - 수신: /app/chat/{chatRoomId}
 * - 브로드캐스트: /topic/chat/{chatRoomId}
 *
 * UseCase 패턴: SendChatMessageUseCase만 호출.
 */
@Controller
class ChatWebSocketController(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val messagingTemplate: SimpMessagingTemplate,
) {

    @MessageMapping("/chat/{chatRoomId}")
    fun handleMessage(
        @DestinationVariable chatRoomId: Long,
        @Payload request: SendMessageRequest,
    ) {
        val result = sendChatMessageUseCase.execute(
            SendChatMessageCommand(
                chatRoomId = chatRoomId,
                senderId = request.senderId,
                content = request.content,
            )
        )
        messagingTemplate.convertAndSend("/topic/chat/$chatRoomId", result)
    }
}
