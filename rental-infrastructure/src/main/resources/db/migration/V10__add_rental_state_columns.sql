-- =============================================================
-- V10__add_rental_state_columns.sql
-- RC-BE-207: rental 테이블 상태 전이 컬럼 추가
-- DB 규칙: FK 금지, ENUM 금지, bool 대신 TINYINT(1), DATETIME(6) 마이크로초 정밀도
-- =============================================================

ALTER TABLE rental
    ADD COLUMN cancel_reason    VARCHAR(500)    NULL            COMMENT '취소/거절 사유' AFTER status,
    ADD COLUMN requested_at     DATETIME(6)     NOT NULL        COMMENT '대여 신청일시' AFTER cancel_reason,
    ADD COLUMN approved_at      DATETIME(6)     NULL            COMMENT '승인일시' AFTER requested_at,
    ADD COLUMN paid_at          DATETIME(6)     NULL            COMMENT '결제완료일시' AFTER approved_at,
    ADD COLUMN started_at       DATETIME(6)     NULL            COMMENT '대여 시작일시' AFTER paid_at,
    ADD COLUMN returned_at      DATETIME(6)     NULL            COMMENT '반납일시' AFTER started_at,
    ADD COLUMN cancelled_at     DATETIME(6)     NULL            COMMENT '취소일시' AFTER returned_at;
