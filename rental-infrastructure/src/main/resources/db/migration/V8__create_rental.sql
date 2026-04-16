-- =============================================================
-- V8__create_rental.sql
-- Sprint 2: 대여(rental) 테이블 생성
-- DB 규칙: FK 금지, ENUM 금지, bool 대신 TINYINT(1), DATETIME(6) 마이크로초 정밀도, COMMENT 필수
-- =============================================================

-- -------------------------------------------------------------
-- rental 테이블
-- -------------------------------------------------------------
CREATE TABLE rental (
    id                          BIGINT          NOT NULL AUTO_INCREMENT   COMMENT '대여 ID',
    renter_id                   BIGINT          NOT NULL                  COMMENT '대여자 user_id',
    lender_id                   BIGINT          NOT NULL                  COMMENT '등록자 user_id',
    product_id                  BIGINT          NOT NULL                  COMMENT '상품 id',
    start_date                  DATETIME(6)     NOT NULL                  COMMENT '대여 시작일시',
    end_date                    DATETIME(6)     NOT NULL                  COMMENT '대여 종료일시',
    total_amount                DECIMAL(15,2)   NOT NULL                  COMMENT '대여료 합계',
    deposit_amount              DECIMAL(15,2)   NOT NULL DEFAULT 0        COMMENT '보증금',
    status                      VARCHAR(30)     NOT NULL                  COMMENT 'REQUESTED|APPROVED|PAID|IN_USE|RETURNED|CANCELLED',
    version                     BIGINT          NOT NULL DEFAULT 0        COMMENT '낙관적 락 버전',
    delivery_recipient_name     VARCHAR(50)     NOT NULL                  COMMENT '배송 수령인 이름',
    delivery_phone              VARCHAR(20)     NOT NULL                  COMMENT '배송 수령인 연락처',
    delivery_address            VARCHAR(300)    NOT NULL                  COMMENT '배송 주소',
    delivery_zip_code           VARCHAR(10)     NOT NULL                  COMMENT '배송 우편번호',
    created_at                  DATETIME(6)     NOT NULL                  COMMENT '생성일시',
    updated_at                  DATETIME(6)     NOT NULL                  COMMENT '수정일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='대여 트랜잭션';

-- -------------------------------------------------------------
-- 인덱스
-- -------------------------------------------------------------
CREATE INDEX idx_rental_renter_id                ON rental (renter_id);
CREATE INDEX idx_rental_lender_id                ON rental (lender_id);
CREATE INDEX idx_rental_product_id_status        ON rental (product_id, status);
