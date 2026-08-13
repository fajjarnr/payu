-- V13__widen_discount_value_to_19_4.sql
-- ARCH-DECIMAL-001: money column must be DECIMAL(19,4) per ADR-0022.
-- Widening only — no data rewrite, no cast hazard (10,4 values fit 19,4).
ALTER TABLE promo_codes
    ALTER COLUMN discount_value TYPE DECIMAL(19, 4);
