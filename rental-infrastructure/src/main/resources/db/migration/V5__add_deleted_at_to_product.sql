-- RC-BE-022: 상품 소프트 삭제 지원을 위한 deleted_at 컬럼 추가
ALTER TABLE product
    ADD COLUMN deleted_at DATETIME(6) NULL COMMENT '소프트 삭제 일시';
