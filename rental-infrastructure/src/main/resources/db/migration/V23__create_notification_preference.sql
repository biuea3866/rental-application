-- =============================================================
-- V23__create_notification_preference.sql
-- RC-DEVOPS-415 / Sprint 4: notification_preference 테이블 + 기본값 백필
-- PRD-004 FR-5 — 채널별 on/off, 신규 가입자 기본값: chat/rental/settlement=ON, marketing=OFF.
-- =============================================================

CREATE TABLE notification_preference (
    id                    BIGINT      NOT NULL AUTO_INCREMENT COMMENT '알림 설정 PK',
    user_id               BIGINT      NOT NULL                COMMENT '사용자 ID',
    chat_enabled          TINYINT(1)  NOT NULL DEFAULT 1      COMMENT '채팅 알림 수신 여부',
    rental_enabled        TINYINT(1)  NOT NULL DEFAULT 1      COMMENT '대여 알림 수신 여부',
    settlement_enabled    TINYINT(1)  NOT NULL DEFAULT 1      COMMENT '정산 알림 수신 여부',
    marketing_enabled     TINYINT(1)  NOT NULL DEFAULT 0      COMMENT '마케팅 알림 수신 여부',
    created_at            DATETIME(6) NOT NULL                COMMENT '생성일시',
    updated_at            DATETIME(6) NOT NULL                COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_pref_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='알림 수신 설정';

-- 기존 가입자 기본값 백필 (legal/PO 결정: marketing OFF 소급 적용)
INSERT INTO notification_preference (user_id, chat_enabled, rental_enabled, settlement_enabled, marketing_enabled, created_at, updated_at)
SELECT u.id, 1, 1, 1, 0, NOW(6), NOW(6)
FROM `user` u
WHERE NOT EXISTS (
    SELECT 1 FROM notification_preference np WHERE np.user_id = u.id
);
