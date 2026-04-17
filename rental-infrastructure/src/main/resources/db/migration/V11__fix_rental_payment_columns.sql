-- =============================================================
-- V11__fix_rental_payment_columns.sql
-- RC-BE-207: rental_payment 테이블 컬럼 정합성 수정
-- - payment_status → status (엔티티 매핑 일치)
-- - order_id, refunded_at 컬럼 추가
-- DB 규칙: FK 금지, ENUM 금지, DATETIME(6) 마이크로초 정밀도
-- =============================================================

-- payment_status 컬럼명을 status로 변경
ALTER TABLE rental_payment
    CHANGE COLUMN payment_status status VARCHAR(30) NOT NULL COMMENT 'PENDING|COMPLETED|FAILED|REFUNDED';

-- order_id, refunded_at 컬럼 추가
ALTER TABLE rental_payment
    ADD COLUMN order_id         VARCHAR(200)    NOT NULL DEFAULT '' COMMENT 'PG 주문 ID (멱등성 키)' AFTER external_payment_id,
    ADD COLUMN refunded_at      DATETIME(6)     NULL COMMENT '환불 완료일시' AFTER paid_at;

-- order_id 인덱스
CREATE INDEX idx_rental_payment_order_id ON rental_payment (order_id);
