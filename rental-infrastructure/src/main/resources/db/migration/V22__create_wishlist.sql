-- =============================================================
-- V22__create_wishlist.sql
-- RC-DEVOPS-415 / Sprint 4: wishlist 테이블 생성
-- PRD-004 FR-4 — 사용자/상품 UNIQUE, 중복 추가 차단.
-- =============================================================

CREATE TABLE wishlist (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '위시리스트 PK',
    user_id     BIGINT      NOT NULL                COMMENT '사용자 ID',
    product_id  BIGINT      NOT NULL                COMMENT '상품 ID',
    created_at  DATETIME(6) NOT NULL                COMMENT '추가 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wishlist_user_product (user_id, product_id),
    INDEX idx_wishlist_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='위시리스트';
