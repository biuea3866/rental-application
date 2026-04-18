package com.rental.commerce.infrastructure.chat.mysql

import com.rental.commerce.domain.chat.ChatRoom
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRoomJpaRepository : JpaRepository<ChatRoom, Long> {

    fun findByRentalId(rentalId: Long): ChatRoom?

    fun findAllByRenterIdOrLenderId(renterId: Long, lenderId: Long): List<ChatRoom>
}
