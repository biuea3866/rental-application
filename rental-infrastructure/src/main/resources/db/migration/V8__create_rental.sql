-- ===================================================
-- V8__create_rental.sql
-- Sprint 2: 대여 테이블 + 배송 정보 테이블 생성
-- ===================================================

CREATE TABLE rental (
    rental_id           BIGINT          NOT NULL AUTO_INCREMENT,
    renter_id           BIGINT          NOT NULL COMMENT '대여자 user_id',
    lender_id           BIGINT          NOT NULL COMMENT '등록자 user_id',
    product_id          BIGINT          NOT NULL COMMENT '상품 id',
    status              VARCHAR(20)     NOT NULL COMMENT 'REQUESTED|APPROVED|PAID|IN_USE|RETURNED|CANCELLED',
    start_date          DATETIME(6)     NOT NULL COMMENT '대여 시작일 (ZonedDateTime → DATETIME(6))',
    end_date            DATETIME(6)     NOT NULL COMMENT '대여 종료일 (ZonedDateTime → DATETIME(6))',
    total_amount        BIGINT          NOT NULL COMMENT '대여료 합계 (원)',
    deposit_amount      BIGINT          NOT NULL DEFAULT 0 COMMENT '보증금 (원)',
    order_id            VARCHAR(100)    NOT NULL COMMENT 'PG orderId (RC-{id}-{ts})',
    cancel_reason       TEXT            NULL,
    recipient_name      VARCHAR(50)     NOT NULL COMMENT '수령인 이름 (DeliveryInfo embedded)',
    recipient_phone     VARCHAR(20)     NOT NULL COMMENT '수령인 연락처',
    address_line1       VARCHAR(200)    NOT NULL COMMENT '도로명/지번 주소',
    address_line2       VARCHAR(100)    NULL COMMENT '상세 주소',
    zip_code            VARCHAR(10)     NOT NULL COMMENT '우편번호',
    requested_at        DATETIME(6)     NOT NULL,
    approved_at         DATETIME(6)     NULL,
    paid_at             DATETIME(6)     NULL,
    started_at          DATETIME(6)     NULL,
    returned_at         DATETIME(6)     NULL,
    cancelled_at        DATETIME(6)     NULL,
    version             BIGINT          NOT NULL DEFAULT 0 COMMENT '낙관적 락 (@Version)',
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,
    PRIMARY KEY (rental_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='대여 트랜잭션';

-- ===================================================
-- 인덱스 (rental)
-- ===================================================

CREATE INDEX idx_rental_renter_id
    ON rental (renter_id);

CREATE INDEX idx_rental_lender_id
    ON rental (lender_id);

CREATE INDEX idx_rental_product_id
    ON rental (product_id);

CREATE INDEX idx_rental_status
    ON rental (status);

CREATE INDEX idx_rental_product_period
    ON rental (product_id, start_date, end_date);

CREATE UNIQUE INDEX uk_rental_order_id
    ON rental (order_id);
