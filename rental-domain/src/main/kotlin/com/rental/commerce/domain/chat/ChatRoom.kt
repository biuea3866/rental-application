package com.rental.commerce.domain.chat

import com.rental.commerce.domain.common.BaseEntity
import com.rental.commerce.domain.common.ChatAccessDeniedException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "chat_room")
class ChatRoom private constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L,

    @Column(name = "rental_id", nullable = false)
    val rentalId: Long,

    @Column(name = "renter_id", nullable = false)
    val renterId: Long,

    @Column(name = "lender_id", nullable = false)
    val lenderId: Long,

) : BaseEntity() {

    fun isParticipant(userId: Long): Boolean = renterId == userId || lenderId == userId

    fun verifyParticipant(userId: Long) {
        if (!isParticipant(userId)) {
            throw ChatAccessDeniedException("채팅방 접근 권한이 없습니다. chatRoomId=$id, userId=$userId")
        }
    }

    companion object {
        fun create(
            rentalId: Long,
            renterId: Long,
            lenderId: Long,
        ): ChatRoom {
            return ChatRoom(
                rentalId = rentalId,
                renterId = renterId,
                lenderId = lenderId,
            )
        }
    }
}
