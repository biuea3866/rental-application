-- =============================================================
-- V18__create_dispute.sql
-- RC-DEVOPS-415 / Sprint 4: dispute 테이블 생성
-- ADR-009 — 분쟁 상태기계. 활성 분쟁(OPEN/UNDER_REVIEW) 1건만 허용.
-- =============================================================

CREATE TABLE dispute (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '분쟁 PK',
    rental_id       BIGINT          NOT NULL                COMMENT '대여 ID',
    opener_id       BIGINT          NOT NULL                COMMENT '분쟁 오픈 사용자 ID (renter or lender)',
    reason          VARCHAR(30)     NOT NULL                COMMENT '분쟁 사유 (DAMAGED/NOT_RETURNED/LATE_RETURN/WRONG_ITEM/OTHER)',
    description     VARCHAR(1000)   NOT NULL                COMMENT '설명 (<=1000자)',
    status          VARCHAR(30)     NOT NULL                COMMENT '상태 (OPEN/UNDER_REVIEW/RESOLVED_REFUND/RESOLVED_PARTIAL/RESOLVED_REJECTED/CANCELLED)',
    refund_amount   DECIMAL(15,2)   NULL                    COMMENT '해결 금액 (PARTIAL일 때만)',
    active_rental_id BIGINT         GENERATED ALWAYS AS (
                                       CASE WHEN status IN ('OPEN','UNDER_REVIEW') THEN rental_id ELSE NULL END
                                    ) STORED                COMMENT '활성 분쟁 감지용 generated column',
    created_at      DATETIME(6)     NOT NULL                COMMENT '생성일시',
    updated_at      DATETIME(6)     NOT NULL                COMMENT '수정일시',
    resolved_at     DATETIME(6)     NULL                    COMMENT '해결 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dispute_active_rental (active_rental_id),
    INDEX idx_dispute_rental (rental_id),
    INDEX idx_dispute_opener (opener_id),
    INDEX idx_dispute_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='분쟁';

CREATE TABLE dispute_attachment (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '첨부 PK',
    dispute_id   BIGINT       NOT NULL                COMMENT '분쟁 ID',
    image_url    VARCHAR(500) NOT NULL                COMMENT 'S3 이미지 URL',
    sort_order   INT          NOT NULL DEFAULT 0      COMMENT '정렬 순서',
    created_at   DATETIME(6)  NOT NULL                COMMENT '생성일시',
    PRIMARY KEY (id),
    INDEX idx_dispute_attachment_dispute (dispute_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='분쟁 첨부 이미지';
