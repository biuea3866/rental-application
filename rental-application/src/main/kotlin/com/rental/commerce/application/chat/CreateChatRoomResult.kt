package com.rental.commerce.application.chat

import com.rental.commerce.domain.chat.ChatRoom
import java.time.ZonedDateTime

data class CreateChatRoomResult(
    val chatRoomId: Long,
    val rentalId: Long,
    val renterId: Long,
    val lenderId: Long,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(chatRoom: ChatRoom): CreateChatRoomResult = CreateChatRoomResult(
            chatRoomId = chatRoom.id,
            rentalId = chatRoom.rentalId,
            renterId = chatRoom.renterId,
            lenderId = chatRoom.lenderId,
            createdAt = chatRoom.createdAt,
        )
    }
}
