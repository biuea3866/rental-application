package com.rental.commerce.application.chat

import com.rental.commerce.domain.chat.ChatMessage
import com.rental.commerce.domain.common.PageResult
import java.time.ZonedDateTime

data class GetChatMessagesResult(
    val messages: PageResult<MessageSummary>,
) {
    data class MessageSummary(
        val messageId: Long,
        val chatRoomId: Long,
        val senderId: Long,
        val content: String,
        val sentAt: ZonedDateTime,
    ) {
        companion object {
            fun from(chatMessage: ChatMessage): MessageSummary = MessageSummary(
                messageId = chatMessage.id,
                chatRoomId = chatMessage.chatRoomId,
                senderId = chatMessage.senderId,
                content = chatMessage.content,
                sentAt = chatMessage.sentAt,
            )
        }
    }

    companion object {
        fun from(pageResult: PageResult<ChatMessage>): GetChatMessagesResult =
            GetChatMessagesResult(
                messages = PageResult(
                    content = pageResult.content.map { MessageSummary.from(it) },
                    totalElements = pageResult.totalElements,
                    totalPages = pageResult.totalPages,
                )
            )
    }
}
