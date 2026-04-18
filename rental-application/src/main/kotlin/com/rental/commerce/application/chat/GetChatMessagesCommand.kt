package com.rental.commerce.application.chat

data class GetChatMessagesCommand(
    val chatRoomId: Long,
    val userId: Long,
    val page: Int,
    val size: Int,
)
