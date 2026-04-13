-- =============================================================================
-- V6: product.current_draft_step 컬럼 타입 TINYINT → INT 변경
-- =============================================================================
-- Hibernate 6(Spring Boot 3.x)는 Kotlin Int를 INTEGER로 매핑.
-- V1 마이그레이션에서 TINYINT로 정의되어 schema-validation 실패.
-- =============================================================================
ALTER TABLE product
    MODIFY COLUMN current_draft_step INT NULL COMMENT '현재 임시저장 단계 (DRAFT 상태일 때만 사용)';
