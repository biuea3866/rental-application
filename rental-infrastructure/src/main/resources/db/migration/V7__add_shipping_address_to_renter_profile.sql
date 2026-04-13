-- =============================================================================
-- V7: renter_profile에 shipping_address 컬럼 추가
-- =============================================================================
-- RenterProfile 엔티티에 shippingAddress 필드 추가 시 마이그레이션 누락
-- =============================================================================
ALTER TABLE renter_profile
    ADD COLUMN shipping_address VARCHAR(255) NULL COMMENT '배송지 주소';
