package com.rental.commerce.presentation.api.chat

import com.rental.commerce.application.chat.SendChatMessageCommand

data class SendMessageRequest(
    val senderId: Long,
    val content: String,
) {
    fun toCommand(chatRoomId: Long): SendChatMessageCommand = SendChatMessageCommand(
        chatRoomId = chatRoomId,
        senderId = senderId,
        content = content,
    )
}
