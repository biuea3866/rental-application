package com.rental.commerce.application.chat

import com.rental.commerce.domain.chat.ChatDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * SendChatMessageUseCase
 *
 * 채팅 메시지 전송 UseCase — ChatDomainService만 호출.
 * WebSocket STOMP 브로드캐스트는 ChatWebSocketController에서 담당.
 */
@Service
@Transactional
class SendChatMessageUseCase(
    private val chatDomainService: ChatDomainService,
) {

    fun execute(command: SendChatMessageCommand): SendChatMessageResult {
        val message = chatDomainService.sendMessage(
            chatRoomId = command.chatRoomId,
            senderId = command.senderId,
            content = command.content,
        )
        return SendChatMessageResult.from(message)
    }
}
