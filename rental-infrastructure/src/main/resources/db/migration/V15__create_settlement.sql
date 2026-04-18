-- =============================================================
-- V15__create_settlement.sql
-- RC-DEVOPS-315: settlement 테이블 생성
-- =============================================================

CREATE TABLE settlement (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '정산 PK',
    lender_id   BIGINT          NOT NULL                COMMENT '대여 등록자 ID',
    rental_id   BIGINT          NOT NULL                COMMENT '대여 ID',
    amount      DECIMAL(15, 2)  NOT NULL                COMMENT '총 대여 금액',
    commission  DECIMAL(15, 2)  NOT NULL                COMMENT '수수료 (10%)',
    net_amount  DECIMAL(15, 2)  NOT NULL                COMMENT '정산 금액 (amount - commission)',
    status      VARCHAR(30)     NOT NULL                COMMENT '정산 상태 (PENDING/COMPLETED/CANCELLED)',
    settled_at  DATETIME(6)     NULL                    COMMENT '정산 완료일시',
    created_at  DATETIME(6)     NOT NULL                COMMENT '생성일시',
    updated_at  DATETIME(6)     NOT NULL                COMMENT '수정일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='정산';

-- 대여 등록자별 정산 조회 인덱스
CREATE INDEX idx_settlement_lender_id ON settlement (lender_id);

-- 대여 ID 기준 정산 조회 인덱스 (중복 정산 방지용도 포함)
CREATE INDEX idx_settlement_rental_id ON settlement (rental_id);
