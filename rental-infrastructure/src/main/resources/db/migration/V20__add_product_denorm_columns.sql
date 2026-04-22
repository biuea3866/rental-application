-- =============================================================
-- V20__add_product_denorm_columns.sql
-- RC-DEVOPS-415 / Sprint 4: product 검색/정렬용 비정규화 컬럼 + 지역 코드 추가
-- ADR-010 — MySQL 유지 전략, 정렬 지표(평점/인기)와 지역 필터를 커버링 인덱스로 처리.
-- 업데이트는 AFTER_COMMIT 리스너로 수행 (BE-411).
-- =============================================================

ALTER TABLE product
    ADD COLUMN rating_avg       DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '평균 평점 (리뷰 생성/수정 AFTER_COMMIT 리스너가 갱신)' AFTER reject_reason,
    ADD COLUMN rating_count     INT          NOT NULL DEFAULT 0    COMMENT '리뷰 건수' AFTER rating_avg,
    ADD COLUMN rental_count     INT          NOT NULL DEFAULT 0    COMMENT '완료된 대여 건수 (RETURNED AFTER_COMMIT 리스너가 갱신)' AFTER rating_count,
    ADD COLUMN region_code      VARCHAR(20)  NULL                   COMMENT '지역 코드 (시·구 등 계층 prefix, 검색 필터용)' AFTER rental_count,
    ADD COLUMN base_price_amount BIGINT      NULL                   COMMENT 'product_price.price_amount 중 DAILY 기본가 비정규화 (검색 정렬/필터용)' AFTER region_code;

-- 기존 row 백필: product_price 의 DAILY price_amount 를 base_price_amount 로 복사 (MIN 으로 중복 방지)
UPDATE product p
LEFT JOIN (
    SELECT product_id, MIN(price_amount) AS min_price
    FROM product_price
    WHERE rental_unit = 'DAILY'
    GROUP BY product_id
) pp ON pp.product_id = p.product_id
SET p.base_price_amount = COALESCE(pp.min_price, 0);
