package com.rental.commerce.application.chat

import com.rental.commerce.domain.chat.ChatMessage
import java.time.ZonedDateTime

data class SendChatMessageResult(
    val messageId: Long,
    val chatRoomId: Long,
    val senderId: Long,
    val content: String,
    val sentAt: ZonedDateTime,
) {
    companion object {
        fun from(chatMessage: ChatMessage): SendChatMessageResult = SendChatMessageResult(
            messageId = chatMessage.id,
            chatRoomId = chatMessage.chatRoomId,
            senderId = chatMessage.senderId,
            content = chatMessage.content,
            sentAt = chatMessage.sentAt,
        )
    }
}
