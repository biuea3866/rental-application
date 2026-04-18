package com.rental.commerce.application.chat

data class CreateChatRoomCommand(
    val rentalId: Long,
    val renterId: Long,
    val lenderId: Long,
)
