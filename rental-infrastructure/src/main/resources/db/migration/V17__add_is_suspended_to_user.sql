-- =============================================================
-- V17__add_is_suspended_to_user.sql
-- RC-BE-311: user 테이블에 is_suspended 컬럼 추가 (관리자 사용자 정지/활성화)
-- =============================================================

ALTER TABLE `user`
    ADD COLUMN is_suspended TINYINT(1) NOT NULL DEFAULT 0 COMMENT '정지 여부' AFTER social_provider_id;
