-- =============================================================
-- V14__create_chat.sql
-- RC-DEVOPS-315: chat_room / chat_message 테이블 생성
-- =============================================================

CREATE TABLE chat_room (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '채팅방 PK',
    rental_id  BIGINT      NOT NULL                COMMENT '대여 ID',
    renter_id  BIGINT      NOT NULL                COMMENT '대여자 ID',
    lender_id  BIGINT      NOT NULL                COMMENT '대여 등록자 ID',
    created_at DATETIME(6) NOT NULL                COMMENT '생성일시',
    updated_at DATETIME(6) NOT NULL                COMMENT '수정일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='채팅방';

-- 대여 ID 기준 채팅방 조회 인덱스
CREATE INDEX idx_chat_room_rental_id ON chat_room (rental_id);

CREATE TABLE chat_message (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '채팅 메시지 PK',
    chat_room_id BIGINT      NOT NULL                COMMENT '채팅방 ID',
    sender_id    BIGINT      NOT NULL                COMMENT '발신자 ID',
    content      TEXT        NOT NULL                COMMENT '메시지 내용',
    sent_at      DATETIME(6) NOT NULL                COMMENT '발신일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='채팅 메시지';

-- 채팅방 내 메시지 시간순 조회 복합 인덱스
CREATE INDEX idx_chat_message_chat_room_id_sent_at ON chat_message (chat_room_id, sent_at);
