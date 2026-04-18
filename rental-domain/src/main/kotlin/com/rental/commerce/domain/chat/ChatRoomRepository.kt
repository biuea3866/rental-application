package com.rental.commerce.domain.chat

interface ChatRoomRepository {

    fun save(chatRoom: ChatRoom): ChatRoom

    fun findById(chatRoomId: Long): ChatRoom?

    fun findByRentalId(rentalId: Long): ChatRoom?

    fun findByUserId(userId: Long): List<ChatRoom>
}
