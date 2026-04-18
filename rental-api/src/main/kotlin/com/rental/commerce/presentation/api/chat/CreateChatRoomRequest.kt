package com.rental.commerce.presentation.api.chat

import com.rental.commerce.application.chat.CreateChatRoomCommand
import jakarta.validation.constraints.NotNull

data class CreateChatRoomRequest(
    @field:NotNull val rentalId: Long?,
    @field:NotNull val renterId: Long?,
    @field:NotNull val lenderId: Long?,
) {
    fun toCommand(userId: Long): CreateChatRoomCommand = CreateChatRoomCommand(
        rentalId = requireNotNull(rentalId),
        renterId = requireNotNull(renterId),
        lenderId = requireNotNull(lenderId),
    )
}
