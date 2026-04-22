-- =============================================================
-- V19__create_refund.sql
-- RC-DEVOPS-415 / Sprint 4: refund 테이블 생성
-- ADR-009 — 분쟁 해결 후 환불 레코드. FK 금지(DB 규칙), 누적 금액 검증은 애플리케이션 + SELECT FOR UPDATE.
-- =============================================================

CREATE TABLE refund (
    id                   BIGINT          NOT NULL AUTO_INCREMENT COMMENT '환불 PK',
    payment_id           BIGINT          NOT NULL                COMMENT 'rental_payment.id 참조 (FK 없음)',
    rental_id            BIGINT          NOT NULL                COMMENT 'rental.id 참조',
    dispute_id           BIGINT          NULL                    COMMENT '연관 분쟁 ID (nullable — 분쟁 외 환불 가능)',
    amount               DECIMAL(15,2)   NOT NULL                COMMENT '환불 금액',
    reason               VARCHAR(100)    NOT NULL                COMMENT '환불 사유 요약',
    status               VARCHAR(30)     NOT NULL                COMMENT 'PENDING|SUCCEEDED|FAILED',
    external_refund_id   VARCHAR(100)    NULL                    COMMENT 'PG 환불 식별자 (Toss refundKey 등)',
    failure_reason       VARCHAR(500)    NULL                    COMMENT 'PG 실패 사유 (FAILED일 때)',
    processed_at         DATETIME(6)     NULL                    COMMENT '환불 완료 일시',
    created_at           DATETIME(6)     NOT NULL                COMMENT '생성일시',
    updated_at           DATETIME(6)     NOT NULL                COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_external_id (external_refund_id),
    INDEX idx_refund_payment (payment_id),
    INDEX idx_refund_rental (rental_id),
    INDEX idx_refund_dispute (dispute_id),
    INDEX idx_refund_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='환불';
