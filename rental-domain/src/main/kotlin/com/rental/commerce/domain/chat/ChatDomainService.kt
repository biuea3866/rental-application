package com.rental.commerce.domain.chat

import com.rental.commerce.domain.common.ChatRoomNotFoundException
import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import org.springframework.stereotype.Service

/**
 * ChatDomainService
 *
 * Entity + Port(ChatRoomRepository / ChatMessageRepository) 조합을 수행하는 도메인 서비스.
 * 참여자 검증은 ChatRoom Entity의 verifyParticipant()에 위임한다.
 *
 * @Transactional 은 UseCase 레이어에서 선언한다 — harness transaction.default 참고.
 */
@Service
class ChatDomainService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatMessageRepository: ChatMessageRepository,
) {

    /**
     * 대여 건에 대한 채팅방을 반환하거나 신규 생성한다.
     * - 이미 존재하면 기존 채팅방 반환
     * - 없으면 신규 채팅방 생성 후 반환
     */
    fun getOrCreateChatRoom(
        rentalId: Long,
        renterId: Long,
        lenderId: Long,
    ): ChatRoom {
        return chatRoomRepository.findByRentalId(rentalId)
            ?: chatRoomRepository.save(
                ChatRoom.create(
                    rentalId = rentalId,
                    renterId = renterId,
                    lenderId = lenderId,
                )
            )
    }

    /**
     * 채팅방에 메시지를 전송한다.
     * - 채팅방 존재 여부 확인
     * - 발신자가 채팅방 참여자인지 검증 (Entity 캡슐화)
     * - 메시지 저장
     */
    fun sendMessage(
        chatRoomId: Long,
        senderId: Long,
        content: String,
    ): ChatMessage {
        val chatRoom = getChatRoomById(chatRoomId)
        chatRoom.verifyParticipant(senderId)
        return chatMessageRepository.save(
            ChatMessage.create(
                chatRoomId = chatRoomId,
                senderId = senderId,
                content = content,
            )
        )
    }

    /**
     * 사용자가 참여 중인 채팅방 목록을 반환한다.
     */
    fun getChatRooms(userId: Long): List<ChatRoom> {
        return chatRoomRepository.findByUserId(userId)
    }

    /**
     * 채팅방의 메시지 목록을 반환한다.
     * - 요청자가 채팅방 참여자인지 검증 (Entity 캡슐화)
     */
    fun getMessages(
        chatRoomId: Long,
        userId: Long,
        pageQuery: PageQuery,
    ): PageResult<ChatMessage> {
        val chatRoom = getChatRoomById(chatRoomId)
        chatRoom.verifyParticipant(userId)
        return chatMessageRepository.findByChatRoomId(chatRoomId, pageQuery)
    }

    // ── private helpers ───────────────────────────────────────────

    private fun getChatRoomById(chatRoomId: Long): ChatRoom {
        return chatRoomRepository.findById(chatRoomId)
            ?: throw ChatRoomNotFoundException("채팅방을 찾을 수 없습니다. chatRoomId=$chatRoomId")
    }
}
