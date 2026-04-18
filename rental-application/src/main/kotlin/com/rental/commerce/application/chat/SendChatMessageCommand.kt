package com.rental.commerce.application.chat

data class SendChatMessageCommand(
    val chatRoomId: Long,
    val senderId: Long,
    val content: String,
)
