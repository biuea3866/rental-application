-- ===================================================
-- V9__create_rental_payment.sql
-- Sprint 2: 대여 결제 테이블 생성
-- ===================================================

CREATE TABLE rental_payment (
    payment_id          BIGINT          NOT NULL AUTO_INCREMENT,
    rental_id           BIGINT          NOT NULL UNIQUE COMMENT '대여 id (1:1 with rental)',
    amount              BIGINT          NOT NULL COMMENT '결제 금액 (원)',
    payment_method      VARCHAR(20)     NOT NULL COMMENT 'CARD|BANK_TRANSFER|TOSS_PAY|KAKAO_PAY',
    status              VARCHAR(20)     NOT NULL COMMENT 'PENDING|COMPLETED|FAILED|REFUNDED',
    external_payment_id VARCHAR(200)    NULL UNIQUE COMMENT 'Toss paymentKey',
    order_id            VARCHAR(100)    NOT NULL UNIQUE COMMENT 'RC-{rentalId}-{ts}',
    paid_at             DATETIME(6)     NULL,
    refunded_at         DATETIME(6)     NULL,
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,
    PRIMARY KEY (payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='대여 결제';

-- ===================================================
-- 인덱스 (rental_payment)
-- ===================================================

CREATE UNIQUE INDEX uk_payment_rental
    ON rental_payment (rental_id);

CREATE UNIQUE INDEX uk_payment_external
    ON rental_payment (external_payment_id);

CREATE UNIQUE INDEX uk_payment_order
    ON rental_payment (order_id);
