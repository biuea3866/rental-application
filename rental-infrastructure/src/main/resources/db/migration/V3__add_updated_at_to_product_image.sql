-- =============================================================
-- V3__add_updated_at_to_product_image.sql
-- product_image 테이블에 updated_at 컬럼 추가 (BaseEntity 호환)
-- =============================================================

ALTER TABLE product_image
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '수정일시'
    AFTER created_at;
