-- =============================================================
-- V12__fix_rental_delivery_columns.sql
-- RC-BE-207: rental 테이블 배송정보 컬럼 정합성 수정
-- - delivery_recipient_name → recipient_name
-- - delivery_phone → recipient_phone
-- - delivery_address → address_line1 + address_line2
-- - delivery_zip_code → zip_code
-- DB 규칙: FK 금지, ENUM 금지, DATETIME(6) 마이크로초 정밀도
-- =============================================================

-- 기존 컬럼 이름 변경
ALTER TABLE rental
    CHANGE COLUMN delivery_recipient_name  recipient_name  VARCHAR(100)    NOT NULL COMMENT '배송 수령인 이름',
    CHANGE COLUMN delivery_phone           recipient_phone VARCHAR(20)     NOT NULL COMMENT '배송 수령인 연락처',
    CHANGE COLUMN delivery_zip_code        zip_code        VARCHAR(10)     NOT NULL COMMENT '배송 우편번호';

-- delivery_address를 address_line1으로 변경 + address_line2 추가
ALTER TABLE rental
    CHANGE COLUMN delivery_address         address_line1   VARCHAR(200)    NOT NULL COMMENT '배송 주소 라인1',
    ADD COLUMN    address_line2            VARCHAR(200)    NULL COMMENT '배송 주소 라인2 (상세주소)' AFTER address_line1;
