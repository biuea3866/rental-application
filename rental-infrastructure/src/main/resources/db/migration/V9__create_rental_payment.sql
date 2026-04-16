-- =============================================================
-- V9__create_rental_payment.sql
-- Sprint 2: 대여 결제(rental_payment) 테이블 생성
-- DB 규칙: FK 금지, ENUM 금지, bool 대신 TINYINT(1), DATETIME(6) 마이크로초 정밀도, COMMENT 필수
-- =============================================================

-- -------------------------------------------------------------
-- rental_payment 테이블
-- -------------------------------------------------------------
CREATE TABLE rental_payment (
    id                      BIGINT          NOT NULL AUTO_INCREMENT   COMMENT '결제 ID',
    rental_id               BIGINT          NOT NULL                  COMMENT '대여 ID (rental.id 참조, FK 없음)',
    amount                  DECIMAL(15,2)   NOT NULL                  COMMENT '결제 금액',
    payment_method          VARCHAR(30)     NOT NULL                  COMMENT 'CARD|BANK_TRANSFER|TOSS_PAY|KAKAO_PAY',
    payment_status          VARCHAR(30)     NOT NULL                  COMMENT 'PENDING|COMPLETED|FAILED|REFUNDED',
    external_payment_id     VARCHAR(100)    NULL                      COMMENT '외부 PG paymentKey (Toss 등)',
    paid_at                 DATETIME(6)     NULL                      COMMENT '결제 완료일시',
    created_at              DATETIME(6)     NOT NULL                  COMMENT '생성일시',
    updated_at              DATETIME(6)     NOT NULL                  COMMENT '수정일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='대여 결제';

-- -------------------------------------------------------------
-- 인덱스
-- -------------------------------------------------------------
CREATE INDEX idx_rental_payment_rental_id        ON rental_payment (rental_id);
CREATE UNIQUE INDEX idx_rental_payment_external_id ON rental_payment (external_payment_id);
