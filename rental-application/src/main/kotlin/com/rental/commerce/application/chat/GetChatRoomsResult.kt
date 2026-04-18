package com.rental.commerce.application.chat

import com.rental.commerce.domain.chat.ChatRoom
import java.time.ZonedDateTime

data class GetChatRoomsResult(
    val chatRooms: List<ChatRoomSummary>,
) {
    data class ChatRoomSummary(
        val chatRoomId: Long,
        val rentalId: Long,
        val renterId: Long,
        val lenderId: Long,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(chatRoom: ChatRoom): ChatRoomSummary = ChatRoomSummary(
                chatRoomId = chatRoom.id,
                rentalId = chatRoom.rentalId,
                renterId = chatRoom.renterId,
                lenderId = chatRoom.lenderId,
                createdAt = chatRoom.createdAt,
            )
        }
    }

    companion object {
        fun from(chatRooms: List<ChatRoom>): GetChatRoomsResult =
            GetChatRoomsResult(chatRooms = chatRooms.map { ChatRoomSummary.from(it) })
    }
}
