package com.rental.commerce.domain.chat

import com.rental.commerce.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "chat_message")
class ChatMessage private constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L,

    @Column(name = "chat_room_id", nullable = false)
    val chatRoomId: Long,

    @Column(name = "sender_id", nullable = false)
    val senderId: Long,

    @Column(name = "content", nullable = false, length = 1000)
    val content: String,

    @Column(name = "sent_at", nullable = false)
    val sentAt: ZonedDateTime,

) : BaseEntity() {

    init {
        require(content.isNotBlank() && content.length <= 1000) {
            "메시지 내용은 1~1000자 사이어야 합니다."
        }
    }

    companion object {
        fun create(
            chatRoomId: Long,
            senderId: Long,
            content: String,
        ): ChatMessage {
            return ChatMessage(
                chatRoomId = chatRoomId,
                senderId = senderId,
                content = content,
                sentAt = ZonedDateTime.now(),
            )
        }
    }
}
