package com.rental.commerce.application.chat

import com.rental.commerce.domain.chat.ChatDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CreateChatRoomUseCase(
    private val chatDomainService: ChatDomainService,
) {

    fun execute(command: CreateChatRoomCommand): CreateChatRoomResult {
        val chatRoom = chatDomainService.getOrCreateChatRoom(
            rentalId = command.rentalId,
            renterId = command.renterId,
            lenderId = command.lenderId,
        )
        return CreateChatRoomResult.from(chatRoom)
    }
}
