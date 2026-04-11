-- =============================================================
-- V1__init_schema.sql
-- Sprint 1: 회원 + 상품 초기 스키마
-- DB 규칙: FK 금지, ENUM 금지, 비정규화 컬럼 금지, BOOLEAN -> TINYINT(1), DATETIME(6), COMMENT 필수
-- =============================================================

-- -------------------------------------------------------------
-- 1. user
-- -------------------------------------------------------------
CREATE TABLE `user` (
    user_id              BIGINT        AUTO_INCREMENT PRIMARY KEY COMMENT '유저 ID',
    email                VARCHAR(100)  NOT NULL       COMMENT '이메일',
    name                 VARCHAR(50)   NOT NULL       COMMENT '이름',
    phone                VARCHAR(20)   NOT NULL       COMMENT '전화번호',
    password_hash        VARCHAR(255)  NOT NULL       COMMENT '비밀번호 해시',
    role                 VARCHAR(20)   NOT NULL       COMMENT '역할 (LENDER, RENTER, BOTH)',
    social_provider      VARCHAR(20)   NULL           COMMENT '소셜 제공자 (KAKAO, NAVER, GOOGLE, APPLE)',
    social_provider_id   VARCHAR(255)  NULL           COMMENT '소셜 제공자 ID',
    created_at           DATETIME(6)   NOT NULL       COMMENT '생성일시',
    updated_at           DATETIME(6)   NOT NULL       COMMENT '수정일시',
    UNIQUE INDEX uk_user_email (email),
    UNIQUE INDEX uk_user_social (social_provider, social_provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='유저';

-- -------------------------------------------------------------
-- 2. lender_profile
-- -------------------------------------------------------------
CREATE TABLE lender_profile (
    lender_profile_id          BIGINT        AUTO_INCREMENT PRIMARY KEY COMMENT '등록자 프로필 ID',
    user_id                    BIGINT        NOT NULL       COMMENT '유저 ID',
    lender_type                VARCHAR(20)   NOT NULL       COMMENT '등록자 유형 (INDIVIDUAL)',
    verification_status        VARCHAR(20)   NOT NULL       COMMENT '인증 상태 (VERIFIED, PENDING)',
    settlement_account_bank    VARCHAR(50)   NULL           COMMENT '정산 계좌 은행',
    settlement_account_number  VARCHAR(50)   NULL           COMMENT '정산 계좌 번호',
    created_at                 DATETIME(6)   NOT NULL       COMMENT '생성일시',
    updated_at                 DATETIME(6)   NOT NULL       COMMENT '수정일시',
    UNIQUE INDEX uk_lender_profile_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='등록자 프로필';

-- -------------------------------------------------------------
-- 3. renter_profile
-- -------------------------------------------------------------
CREATE TABLE renter_profile (
    renter_profile_id       BIGINT        AUTO_INCREMENT PRIMARY KEY COMMENT '대여자 프로필 ID',
    user_id                 BIGINT        NOT NULL       COMMENT '유저 ID',
    trust_grade             VARCHAR(20)   NOT NULL       COMMENT '신뢰 등급 (BRONZE, SILVER, GOLD)',
    total_transaction_count INT           NOT NULL DEFAULT 0 COMMENT '총 거래 수',
    created_at              DATETIME(6)   NOT NULL       COMMENT '생성일시',
    updated_at              DATETIME(6)   NOT NULL       COMMENT '수정일시',
    UNIQUE INDEX uk_renter_profile_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='대여자 프로필';

-- -------------------------------------------------------------
-- 4. product
-- -------------------------------------------------------------
CREATE TABLE product (
    product_id         BIGINT        AUTO_INCREMENT PRIMARY KEY COMMENT '상품 ID',
    user_id            BIGINT        NOT NULL       COMMENT '등록자 유저 ID',
    name               VARCHAR(100)  NULL           COMMENT '상품명',
    description        TEXT          NULL           COMMENT '상품 설명',
    category_code      VARCHAR(50)   NULL           COMMENT '카테고리 코드',
    `condition`        VARCHAR(20)   NULL           COMMENT '상품 상태 (NEW, LIKE_NEW, GOOD, FAIR)',
    status             VARCHAR(20)   NOT NULL       COMMENT '상품 진행 상태 (DRAFT, UNDER_REVIEW, APPROVED, REJECTED, AVAILABLE, RENTED)',
    current_draft_step TINYINT       NULL           COMMENT '현재 임시저장 단계 (DRAFT 상태일 때만 사용)',
    deposit_amount     BIGINT        NULL           COMMENT '보증금',
    reject_reason      VARCHAR(500)  NULL           COMMENT '반려 사유',
    created_at         DATETIME(6)   NOT NULL       COMMENT '생성일시',
    updated_at         DATETIME(6)   NOT NULL       COMMENT '수정일시',
    INDEX idx_product_user_id (user_id),
    INDEX idx_product_status_category (status, category_code),
    INDEX idx_product_category (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='상품';

-- -------------------------------------------------------------
-- 5. product_price
-- -------------------------------------------------------------
CREATE TABLE product_price (
    product_price_id BIGINT        AUTO_INCREMENT PRIMARY KEY COMMENT '상품 가격 ID',
    product_id       BIGINT        NOT NULL       COMMENT '상품 ID',
    rental_unit      VARCHAR(20)   NOT NULL       COMMENT '대여 단위 (DAILY, MONTHLY, YEARLY)',
    price_amount     BIGINT        NOT NULL       COMMENT '가격',
    created_at       DATETIME(6)   NOT NULL       COMMENT '생성일시',
    updated_at       DATETIME(6)   NOT NULL       COMMENT '수정일시',
    INDEX idx_product_price_product (product_id),
    UNIQUE INDEX uk_product_price_unit (product_id, rental_unit)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='상품 가격';

-- -------------------------------------------------------------
-- 6. product_image
-- -------------------------------------------------------------
CREATE TABLE product_image (
    product_image_id  BIGINT        AUTO_INCREMENT PRIMARY KEY COMMENT '상품 이미지 ID',
    product_id        BIGINT        NOT NULL       COMMENT '상품 ID',
    object_key        VARCHAR(500)  NOT NULL       COMMENT 'MinIO 오브젝트 키',
    original_filename VARCHAR(255)  NOT NULL       COMMENT '원본 파일명',
    sort_order        SMALLINT      NOT NULL       COMMENT '정렬 순서',
    created_at        DATETIME(6)   NOT NULL       COMMENT '생성일시',
    INDEX idx_product_image_product_sort (product_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='상품 이미지';

-- -------------------------------------------------------------
-- 7. category_price_guide
-- -------------------------------------------------------------
CREATE TABLE category_price_guide (
    category_price_guide_id BIGINT        AUTO_INCREMENT PRIMARY KEY COMMENT '카테고리 가이드 가격 ID',
    category_code           VARCHAR(50)   NOT NULL       COMMENT '카테고리 코드',
    rental_unit             VARCHAR(20)   NOT NULL       COMMENT '대여 단위 (DAILY, MONTHLY, YEARLY)',
    min_price               BIGINT        NOT NULL       COMMENT '최소 가격',
    max_price               BIGINT        NOT NULL       COMMENT '최대 가격',
    created_at              DATETIME(6)   NOT NULL       COMMENT '생성일시',
    updated_at              DATETIME(6)   NOT NULL       COMMENT '수정일시',
    UNIQUE INDEX uk_category_price_guide (category_code, rental_unit)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='카테고리 가이드 가격';
