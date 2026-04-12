CREATE TABLE notification (
    notification_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '알림 ID',
    user_id BIGINT NOT NULL COMMENT '수신자 ID',
    title VARCHAR(200) NOT NULL COMMENT '알림 제목',
    message TEXT NOT NULL COMMENT '알림 내용',
    notification_type VARCHAR(30) NOT NULL COMMENT '알림 유형',
    reference_id BIGINT COMMENT '참조 엔티티 ID',
    reference_type VARCHAR(30) COMMENT '참조 엔티티 유형',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '읽음 여부',
    created_at DATETIME(6) NOT NULL COMMENT '생성일시',
    updated_at DATETIME(6) NOT NULL COMMENT '수정일시',
    deleted_at DATETIME(6) NULL DEFAULT NULL COMMENT '삭제일시(소프트 삭제)',
    PRIMARY KEY (notification_id),
    INDEX idx_notification_user_read (user_id, is_read),
    INDEX idx_notification_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='알림';
