package com.rental.commerce.infrastructure.chat.mysql

import com.rental.commerce.domain.chat.ChatMessage
import com.rental.commerce.domain.chat.ChatMessageRepository
import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class ChatMessageRepositoryImpl(
    private val chatMessageJpaRepository: ChatMessageJpaRepository,
) : ChatMessageRepository {

    override fun save(chatMessage: ChatMessage): ChatMessage {
        return chatMessageJpaRepository.save(chatMessage)
    }

    override fun findByChatRoomId(chatRoomId: Long, pageQuery: PageQuery): PageResult<ChatMessage> {
        val pageable = PageRequest.of(
            pageQuery.page,
            pageQuery.size,
            Sort.by(Sort.Direction.ASC, "sentAt"),
        )
        val page = chatMessageJpaRepository.findAllByChatRoomId(chatRoomId, pageable)
        return PageResult(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
