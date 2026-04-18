package com.rental.commerce.application.chat

import com.rental.commerce.domain.chat.ChatDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetChatRoomsUseCase(
    private val chatDomainService: ChatDomainService,
) {

    fun execute(command: GetChatRoomsCommand): GetChatRoomsResult {
        val chatRooms = chatDomainService.getChatRooms(userId = command.userId)
        return GetChatRoomsResult.from(chatRooms)
    }
}
