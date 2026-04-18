-- =============================================================
-- V13__create_review.sql
-- RC-DEVOPS-315: review 테이블 생성
-- =============================================================

CREATE TABLE review (
    id         BIGINT          NOT NULL AUTO_INCREMENT COMMENT '리뷰 PK',
    renter_id  BIGINT          NOT NULL                COMMENT '리뷰 작성자(대여자) ID',
    rental_id  BIGINT          NOT NULL                COMMENT '대여 ID',
    product_id BIGINT          NOT NULL                COMMENT '상품 ID',
    rating     INT             NOT NULL                COMMENT '평점 (1~5)',
    content    TEXT            NOT NULL                COMMENT '리뷰 내용',
    created_at DATETIME(6)     NOT NULL                COMMENT '생성일시',
    updated_at DATETIME(6)     NOT NULL                COMMENT '수정일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='리뷰';

-- 상품별 리뷰 조회 인덱스
CREATE INDEX idx_review_product_id ON review (product_id);

-- 대여자별 리뷰 조회 인덱스
CREATE INDEX idx_review_renter_id ON review (renter_id);

-- 대여 건당 중복 리뷰 방지 UNIQUE 인덱스
CREATE UNIQUE INDEX idx_review_rental_id ON review (rental_id);
