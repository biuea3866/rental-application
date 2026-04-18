package com.rental.commerce.domain.chat

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult

interface ChatMessageRepository {

    fun save(chatMessage: ChatMessage): ChatMessage

    fun findByChatRoomId(chatRoomId: Long, pageQuery: PageQuery): PageResult<ChatMessage>
}
