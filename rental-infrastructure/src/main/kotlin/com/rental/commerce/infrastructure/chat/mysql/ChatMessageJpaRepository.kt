package com.rental.commerce.infrastructure.chat.mysql

import com.rental.commerce.domain.chat.ChatMessage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ChatMessageJpaRepository : JpaRepository<ChatMessage, Long> {

    fun findAllByChatRoomId(chatRoomId: Long, pageable: Pageable): Page<ChatMessage>
}
