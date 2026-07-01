-- AUDIT-042 / GAP-25: Upgrade monetary columns from NUMERIC(19,2) to DECIMAL(19,4).
-- AGENTS.md Rule #1: BigDecimal HALF_EVEN, DB DECIMAL(19,4). Money columns MUST use 4 fractional
-- digits to match the core ledger precision in wallet-service.
--
-- Affected columns (partner-service):
--   - payment_links.amount            (V5__create_payment_links_table.sql:8)
--   - merchant_qr_payments.amount     (V6__create_merchant_and_qr_tables.sql:34)
--   - snap_bi_payments.amount         (V8__create_snap_bi_payment_tables.sql:9)
--   - snap_bi_refunds.amount          (V8__create_snap_bi_payment_tables.sql:28)
--
-- USAGE: NUMERIC and DECIMAL are aliases in PostgreSQL. Using DECIMAL(19,4) for consistency.

BEGIN;

ALTER TABLE payment_links
    ALTER COLUMN amount TYPE DECIMAL(19,4) USING amount::DECIMAL(19,4);

ALTER TABLE merchant_qr_payments
    ALTER COLUMN amount TYPE DECIMAL(19,4) USING amount::DECIMAL(19,4);

ALTER TABLE snap_bi_payments
    ALTER COLUMN amount TYPE DECIMAL(19,4) USING amount::DECIMAL(19,4);

ALTER TABLE snap_bi_refunds
    ALTER COLUMN amount TYPE DECIMAL(19,4) USING amount::DECIMAL(19,4);

COMMIT;
