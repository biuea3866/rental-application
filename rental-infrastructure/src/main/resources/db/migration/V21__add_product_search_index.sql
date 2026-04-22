-- =============================================================
-- V21__add_product_search_index.sql
-- RC-DEVOPS-415 / Sprint 4: product 커버링 인덱스 (ADR-010)
-- 검색 필터 기본 축: status → category_code → region_code → price_amount
-- 정렬 보조 인덱스: rating_avg, rental_count, created_at
-- =============================================================

-- 기존 idx_product_status_category (status, category_code) 를 확장 대체
ALTER TABLE product DROP INDEX idx_product_status_category;

CREATE INDEX idx_product_search
    ON product (status, category_code, region_code, price_amount);

CREATE INDEX idx_product_rating
    ON product (status, rating_avg);

CREATE INDEX idx_product_rental_count
    ON product (status, rental_count);
