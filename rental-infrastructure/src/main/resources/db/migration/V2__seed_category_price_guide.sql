-- =============================================================
-- V2__seed_category_price_guide.sql
-- 카테고리별 가이드 가격 초기 데이터
-- 각 카테고리: DAILY, MONTHLY, YEARLY (3행)
-- =============================================================

INSERT INTO category_price_guide (category_code, rental_unit, min_price, max_price, created_at, updated_at) VALUES
-- 전자기기
('ELECTRONICS', 'DAILY',     5000,    50000, NOW(6), NOW(6)),
('ELECTRONICS', 'MONTHLY',  50000,   500000, NOW(6), NOW(6)),
('ELECTRONICS', 'YEARLY',  500000,  5000000, NOW(6), NOW(6)),

-- 캠핑
('CAMPING', 'DAILY',     3000,    30000, NOW(6), NOW(6)),
('CAMPING', 'MONTHLY',  30000,   300000, NOW(6), NOW(6)),
('CAMPING', 'YEARLY',  300000,  3000000, NOW(6), NOW(6)),

-- 의류
('CLOTHING', 'DAILY',     2000,    20000, NOW(6), NOW(6)),
('CLOTHING', 'MONTHLY',  20000,   200000, NOW(6), NOW(6)),
('CLOTHING', 'YEARLY',  200000,  2000000, NOW(6), NOW(6)),

-- 가구
('FURNITURE', 'DAILY',     5000,    30000, NOW(6), NOW(6)),
('FURNITURE', 'MONTHLY',  50000,   300000, NOW(6), NOW(6)),
('FURNITURE', 'YEARLY',  500000,  3000000, NOW(6), NOW(6)),

-- 스포츠
('SPORTS', 'DAILY',     3000,    20000, NOW(6), NOW(6)),
('SPORTS', 'MONTHLY',  30000,   200000, NOW(6), NOW(6)),
('SPORTS', 'YEARLY',  300000,  2000000, NOW(6), NOW(6)),

-- 도서
('BOOKS', 'DAILY',      500,     3000, NOW(6), NOW(6)),
('BOOKS', 'MONTHLY',   5000,    30000, NOW(6), NOW(6)),
('BOOKS', 'YEARLY',   50000,   300000, NOW(6), NOW(6)),

-- 기타
('ETC', 'DAILY',     1000,    50000, NOW(6), NOW(6)),
('ETC', 'MONTHLY',  10000,   500000, NOW(6), NOW(6)),
('ETC', 'YEARLY',  100000,  5000000, NOW(6), NOW(6));
