-- V5__add_version_to_billing_entities.sql
-- ITER-52B: Add @Version column to 4 billing entities for optimistic locking.

ALTER TABLE subscription_plans     ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE bill_payments         ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE subscription_charges  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE subscriptions         ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE subscription_plans     SET version = 0 WHERE version IS NULL;
UPDATE bill_payments         SET version = 0 WHERE version IS NULL;
UPDATE subscription_charges  SET version = 0 WHERE version IS NULL;
UPDATE subscriptions         SET version = 0 WHERE version IS NULL;
