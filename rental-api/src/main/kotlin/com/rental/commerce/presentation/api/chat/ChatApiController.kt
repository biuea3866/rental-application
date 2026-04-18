package com.rental.commerce.presentation.api.chat

import com.rental.commerce.application.chat.CreateChatRoomResult
import com.rental.commerce.application.chat.CreateChatRoomUseCase
import com.rental.commerce.application.chat.GetChatMessagesCommand
import com.rental.commerce.application.chat.GetChatMessagesResult
import com.rental.commerce.application.chat.GetChatMessagesUseCase
import com.rental.commerce.application.chat.GetChatRoomsCommand
import com.rental.commerce.application.chat.GetChatRoomsResult
import com.rental.commerce.application.chat.GetChatRoomsUseCase
import com.rental.commerce.presentation.api.common.AuthenticatedMember
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class ChatApiController(
    private val createChatRoomUseCase: CreateChatRoomUseCase,
    private val getChatRoomsUseCase: GetChatRoomsUseCase,
    private val getChatMessagesUseCase: GetChatMessagesUseCase,
) {

    @PostMapping("/chat-rooms")
    fun createChatRoom(
        @AuthenticatedMember userId: Long,
        @Valid @RequestBody request: CreateChatRoomRequest,
    ): ResponseEntity<CreateChatRoomResult> {
        val result = createChatRoomUseCase.execute(request.toCommand(userId))
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @GetMapping("/chat-rooms")
    fun getChatRooms(
        @AuthenticatedMember userId: Long,
    ): ResponseEntity<GetChatRoomsResult> {
        val result = getChatRoomsUseCase.execute(GetChatRoomsCommand(userId = userId))
        return ResponseEntity.ok(result)
    }

    @GetMapping("/chat-rooms/{chatRoomId}/messages")
    fun getChatMessages(
        @AuthenticatedMember userId: Long,
        @PathVariable chatRoomId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<GetChatMessagesResult> {
        val result = getChatMessagesUseCase.execute(
            GetChatMessagesCommand(
                chatRoomId = chatRoomId,
                userId = userId,
                page = page,
                size = size,
            )
        )
        return ResponseEntity.ok(result)
    }
}
