-- =============================================================
-- V16__add_settlement_unique_constraint.sql
-- RC-BE-309: settlement.rental_id UNIQUE INDEX 추가 (중복 정산 방지)
-- =============================================================

CREATE UNIQUE INDEX uk_settlement_rental_id ON settlement (rental_id);
