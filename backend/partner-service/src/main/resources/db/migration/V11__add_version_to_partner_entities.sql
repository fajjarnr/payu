-- V11__add_version_to_partner_entities.sql
-- ITER-52: Add @Version column (optimistic locking) to partner-service entities.

ALTER TABLE webhook_deliveries                  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE merchant_qr_payments                ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE webhook_subscriptions               ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE merchants                           ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE snap_bi_payments                    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE api_keys                            ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE partner_certificates                ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE partners                            ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE snap_bi_refunds                     ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE payment_links                       ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE webhook_deliveries                  SET version = 0 WHERE version IS NULL;
UPDATE merchant_qr_payments                SET version = 0 WHERE version IS NULL;
UPDATE webhook_subscriptions               SET version = 0 WHERE version IS NULL;
UPDATE merchants                           SET version = 0 WHERE version IS NULL;
UPDATE snap_bi_payments                    SET version = 0 WHERE version IS NULL;
UPDATE api_keys                            SET version = 0 WHERE version IS NULL;
UPDATE partner_certificates                SET version = 0 WHERE version IS NULL;
UPDATE partners                            SET version = 0 WHERE version IS NULL;
UPDATE snap_bi_refunds                     SET version = 0 WHERE version IS NULL;
UPDATE payment_links                       SET version = 0 WHERE version IS NULL;
