package com.rental.commerce.infrastructure.chat.mysql

import com.rental.commerce.domain.chat.ChatRoom
import com.rental.commerce.domain.chat.ChatRoomRepository
import org.springframework.stereotype.Repository

@Repository
class ChatRoomRepositoryImpl(
    private val chatRoomJpaRepository: ChatRoomJpaRepository,
) : ChatRoomRepository {

    override fun save(chatRoom: ChatRoom): ChatRoom {
        return chatRoomJpaRepository.save(chatRoom)
    }

    override fun findById(chatRoomId: Long): ChatRoom? {
        return chatRoomJpaRepository.findById(chatRoomId).orElse(null)
    }

    override fun findByRentalId(rentalId: Long): ChatRoom? {
        return chatRoomJpaRepository.findByRentalId(rentalId)
    }

    override fun findByUserId(userId: Long): List<ChatRoom> {
        return chatRoomJpaRepository.findAllByRenterIdOrLenderId(userId, userId)
    }
}
