package com.rental.commerce.application.chat

import com.rental.commerce.domain.chat.ChatDomainService
import com.rental.commerce.domain.common.PageQuery
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetChatMessagesUseCase(
    private val chatDomainService: ChatDomainService,
) {

    fun execute(command: GetChatMessagesCommand): GetChatMessagesResult {
        val pageResult = chatDomainService.getMessages(
            chatRoomId = command.chatRoomId,
            userId = command.userId,
            pageQuery = PageQuery(page = command.page, size = command.size),
        )
        return GetChatMessagesResult.from(pageResult)
    }
}
